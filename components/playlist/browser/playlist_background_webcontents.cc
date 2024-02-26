/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/playlist/browser/playlist_background_webcontents.h"

#include "base/functional/bind.h"
#include "brave/components/playlist/browser/playlist_background_webcontents_helper.h"
#include "brave/components/playlist/browser/playlist_service.h"
#include "brave/components/playlist/common/features.h"
#include "content/public/browser/navigation_entry.h"
#include "content/public/browser/web_contents.h"

namespace playlist {

PlaylistBackgroundWebContents::PlaylistBackgroundWebContents(
    content::BrowserContext* context,
    PlaylistService* service)
    : context_(context), service_(service) {}

PlaylistBackgroundWebContents::~PlaylistBackgroundWebContents() = default;

void PlaylistBackgroundWebContents::Add(
    const GURL& url,
    base::OnceCallback<PlaylistMediaHandler::Signature>
        on_detected_media_callback) {
  content::WebContents::CreateParams create_params(context_);
  create_params.is_never_visible = true;

  auto web_contents = content::WebContents::Create(create_params);
  auto* web_contents_raw = web_contents.get();
  web_contents->SetAudioMuted(true);

  PlaylistMediaHandler::CreateForWebContents(
      web_contents_raw,
      base::BindOnce(&PlaylistBackgroundWebContents::Remove,
                     weak_factory_.GetWeakPtr(), web_contents_raw,
                     std::move(on_detected_media_callback)));
  PlaylistBackgroundWebContentsHelper::CreateForWebContents(
      web_contents_raw, service_->GetMediaSourceAPISuppressorScript(),
      service_->GetMediaDetectorScript(url));

  auto load_url_params = content::NavigationController::LoadURLParams(url);

  content::NavigationController& controller = web_contents->GetController();
  if (service_->ShouldUseFakeUA(url) ||
      base::FeatureList::IsEnabled(features::kPlaylistFakeUA)) {
    DVLOG(2) << __func__ << " Faked UA to detect media files";

    blink::UserAgentOverride user_agent(
        "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) "
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 "
        "Mobile/15E148 "
        "Safari/604.1",
        /* user_agent_metadata */ {});
    web_contents->SetUserAgentOverride(user_agent,
                                       /* override_in_new_tabs= */ true);
    load_url_params.override_user_agent =
        content::NavigationController::UA_OVERRIDE_TRUE;

    for (int i = 0; i < controller.GetEntryCount(); ++i) {
      controller.GetEntryAtIndex(i)->SetIsOverridingUserAgent(true);
    }
  }

  controller.LoadURLWithParams(load_url_params);

  background_web_contents_.emplace(web_contents_raw, std::move(web_contents));
}

void PlaylistBackgroundWebContents::Remove(
    content::WebContents* web_contents,
    base::OnceCallback<PlaylistMediaHandler::Signature>
        on_detected_media_callback,
    base::Value::List media,
    const GURL& url) {
  CHECK(background_web_contents_.erase(web_contents));
  std::move(on_detected_media_callback).Run(std::move(media), url);
}

}  // namespace playlist
