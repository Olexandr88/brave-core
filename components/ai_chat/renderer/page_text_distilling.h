// Copyright (c) 2023 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// You can obtain one at https://mozilla.org/MPL/2.0/.

#ifndef BRAVE_COMPONENTS_AI_CHAT_RENDERER_PAGE_TEXT_DISTILLING_H_
#define BRAVE_COMPONENTS_AI_CHAT_RENDERER_PAGE_TEXT_DISTILLING_H_

#include <optional>
#include <string>

#include "base/functional/callback_forward.h"
#include "base/values.h"

namespace content {
class RenderFrame;
}

namespace ai_chat {

bool LoadSiteScriptForHost(const std::string& host,
                           std::string* script_content);

void OnSiteScriptExecutionResult(
    base::OnceCallback<void(const std::optional<std::string>&)>,
    std::optional<base::Value> result);

void DistillPageText(
    content::RenderFrame* render_frame,
    int32_t global_world_id,
    int32_t isolated_world_id,
    base::OnceCallback<void(const std::optional<std::string>&)>);

}  // namespace ai_chat

#endif  // BRAVE_COMPONENTS_AI_CHAT_RENDERER_PAGE_TEXT_DISTILLING_H_
