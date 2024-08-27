/* Copyright (c) 2023 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_AI_CHAT_CORE_BROWSER_ASSOCIATED_CONTENT_DRIVER_H_
#define BRAVE_COMPONENTS_AI_CHAT_CORE_BROWSER_ASSOCIATED_CONTENT_DRIVER_H_

#include <memory>
#include <set>
#include <string>
#include <string_view>

#include "base/gtest_prod_util.h"
#include "base/memory/raw_ptr.h"
#include "base/one_shot_event.h"
#include "brave/components/ai_chat/core/browser/ai_chat_credential_manager.h"
#include "brave/components/ai_chat/core/browser/ai_chat_service.h"
#include "brave/components/ai_chat/core/browser/conversation_handler.h"
#include "brave/components/ai_chat/core/browser/model_service.h"

class PrefService;

FORWARD_DECLARE_TEST(AIChatUIBrowserTest, PrintPreviewFallback);
namespace ai_chat {
class AIChatMetrics;

class AssociatedContentDriver
    : public ConversationHandler::AssociatedContentDelegate {
 public:
  class Observer : public base::CheckedObserver {
   public:
    ~Observer() override {}

    virtual void OnAssociatedContentNavigated(int new_navigation_id) {}
  };

  AssociatedContentDriver();
  ~AssociatedContentDriver() override;

  AssociatedContentDriver(const AssociatedContentDriver&) = delete;
  AssociatedContentDriver& operator=(const AssociatedContentDriver&) = delete;

  void AddObserver(Observer* observer);
  void RemoveObserver(Observer* observer);

  // ConversationHandler::AssociatedContentDelegate
  void AddRelatedConversation(ConversationHandler* conversation) override;
  void OnRelatedConversationDestroyed(
      ConversationHandler* conversation) override;
  int GetContentId() const override;
  GURL GetURL() const override;
  std::u16string GetTitle() const override;
  void GetContent(
      ConversationHandler::GetPageContentCallback callback) override;
  std::string_view GetCachedTextContent() override;
  bool GetCachedIsVideo() override;
  //   // Implementer should use alternative method of page content fetching
  // void PrintPreviewFallback(ConversationHandler::GetPageContentCallback
  // callback) override;

  base::WeakPtr<AssociatedContentDriver> GetWeakPtr() {
    return weak_ptr_factory_.GetWeakPtr();
  }

 protected:
  virtual GURL GetPageURL() const = 0;
  virtual std::u16string GetPageTitle() const = 0;

  // Implementer should fetch content from the "page" associated with this
  // conversation.
  // |invalidation_token| is an optional parameter received in a prior callback
  // response of this function against the same page. See GetPageContentCallback
  // for explanation.
  virtual void GetPageContent(
      ConversationHandler::GetPageContentCallback callback,
      std::string_view invalidation_token) = 0;

  virtual void OnFaviconImageDataChanged();

  // Implementer should call this when the content is updated in a way that
  // will not be detected by the on-demand techniques used by GetPageContent.
  // For example for sites where GetPageContent does not read the live DOM but
  // reads static JS from HTML that doesn't change for same-page navigation and
  // we need to intercept new JS data from subresource loads.
  void OnPageContentUpdated(std::string content,
                            bool is_video,
                            std::string invalidation_token);

  // To be called when a page navigation is detected and a new conversation
  // is expected.
  void OnNewPage(int64_t navigation_id) override;

  // Begin the alternative content fetching (print preview / OCR) by
  // sending a message to an observer with access to the layer this can
  // be performed on (i.e. the UI layer can use Print Preview).
  // Since this AssociatedContentDriver might be open in multiple WebUIs,
  // we can no longer rely on the WebUI class observing its conversation 1:1.
  // We might end up with multiple UIs performing the print preview fetching
  // for a single conversation. Instead we need some single entity that is
  // able to perform this work, either by relocating AIChatTabHelper to
  // brave/browser/ui, or having a singleton/KeyedService observe this event
  // void NotifyPrintPreviewRequested(bool is_pdf);
 private:
  FRIEND_TEST_ALL_PREFIXES(::AIChatUIBrowserTest, PrintPreviewFallback);
  FRIEND_TEST_ALL_PREFIXES(AssociatedContentDriverUnitTest,
                           UpdateOrCreateLastAssistantEntry_Delta);
  FRIEND_TEST_ALL_PREFIXES(AssociatedContentDriverUnitTest,
                           UpdateOrCreateLastAssistantEntry_DeltaWithSearch);
  FRIEND_TEST_ALL_PREFIXES(AssociatedContentDriverUnitTest,
                           UpdateOrCreateLastAssistantEntry_NotDelta);
  FRIEND_TEST_ALL_PREFIXES(AssociatedContentDriverUnitTest,
                           UpdateOrCreateLastAssistantEntry_NotDeltaWithSearch);

  void OnGeneratePageContentComplete(int64_t navigation_id,
                                     std::string contents_text,
                                     bool is_video,
                                     std::string invalidation_token);
  void OnExistingGeneratePageContentComplete(
      ConversationHandler::GetPageContentCallback callback,
      int64_t navigation_id);

  raw_ptr<PrefService> pref_service_;
  raw_ptr<AIChatMetrics> ai_chat_metrics_;
  std::unique_ptr<AIChatCredentialManager> credential_manager_;

  scoped_refptr<network::SharedURLLoaderFactory> url_loader_factory_;

  base::ObserverList<Observer> observers_;
  std::unique_ptr<base::OneShotEvent> on_page_text_fetch_complete_;
  bool is_page_text_fetch_in_progress_ = false;
  std::string cached_text_content_;
  std::string content_invalidation_token_;
  bool is_video_ = false;

  // Handlers that are interested in this content for the current navigation.
  std::set<raw_ptr<ConversationHandler>> associated_conversations_;

  // Store the unique ID for each "page" so that
  // we can ignore API async responses against any navigated-away-from
  // documents.
  int64_t current_navigation_id_;

  base::WeakPtrFactory<AssociatedContentDriver> weak_ptr_factory_{this};
};

}  // namespace ai_chat

#endif  // BRAVE_COMPONENTS_AI_CHAT_CORE_BROWSER_ASSOCIATED_CONTENT_DRIVER_H_
