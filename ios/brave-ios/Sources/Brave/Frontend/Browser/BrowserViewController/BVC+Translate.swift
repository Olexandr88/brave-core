// Copyright 2024 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

import BraveUI
import DesignSystem
import Foundation
import Onboarding
import Preferences
import UIKit
import SwiftUI

extension BrowserViewController: BraveTranslateScriptHandlerDelegate {
  func updateTranslateURLBar(tab: Tab?, state: TranslateURLBarButton.TranslateState) {
    guard let tab = tab else { return }

    if tab === tabManager.selectedTab {
      tab.translationState = state

      //      translateActivity(info: state == .existingItem ? item : nil)
      topToolbar.updateTranslateButtonState(state)
    }
  }

  func showTranslateOnboarding(tab: Tab?, completion: @escaping (_ translateEnabled: Bool) -> Void)
  {
    // Do NOT show the translate onboarding popup if the tab isn't visible
    guard Preferences.Translate.translateEnabled.value,
      let selectedTab = tabManager.selectedTab,
      selectedTab === tab,
      selectedTab.translationState == .available
    else {
      completion(Preferences.Translate.translateEnabled.value)
      return
    }

    if Preferences.Translate.translateURLBarOnboardingCount.value < 2,
      shouldShowTranslationOnboardingThisSession,
      presentedViewController == nil
    {
      Preferences.Translate.translateURLBarOnboardingCount.value += 1

      topToolbar.layoutIfNeeded()
      view.layoutIfNeeded()

      // Ensure url bar is expanded before presenting a popover on it
      toolbarVisibilityViewModel.toolbarState = .expanded

      DispatchQueue.main.async {
        let popover = PopoverController(
          content: OnboardingTranslateView(
            onContinueButtonPressed: { [weak self, weak tab] in
              guard let tab = tab, let self = self else { return }

              self.topToolbar.locationView.translateButton.setOnboardingState(enabled: false)

              if let scriptHandler = tab.getContentScript(
                name: BraveTranslateScriptHandler.scriptName
              )
                as? BraveTranslateScriptHandler
              {
                scriptHandler.presentUI(on: self)
              }
            },
            onDisableFeature: { [weak self, weak tab] in
              guard let tab = tab, let self = self else { return }

              self.topToolbar.locationView.translateButton.setOnboardingState(enabled: false)

              Preferences.Translate.translateEnabled.value = false
              tab.translationState = .unavailable
              self.topToolbar.updateTranslateButtonState(.unavailable)
            }
          ),
          autoLayoutConfiguration: .init(preferredWidth: self.view.bounds.width - (32.0 * 2.0))
        )

        popover.arrowDistance = 10.0

        popover.previewForOrigin = .init(
          view: self.topToolbar.locationView.translateButton.then {
            $0.setOnboardingState(enabled: true)
          },
          action: { popover in
            popover.previewForOrigin = nil
            popover.dismissPopover()
            completion(Preferences.Translate.translateEnabled.value)
          }
        )

        popover.popoverDidDismiss = { [weak self] _ in
          self?.topToolbar.locationView.translateButton.setOnboardingState(enabled: false)
          completion(Preferences.Translate.translateEnabled.value)
        }
        popover.present(from: self.topToolbar.locationView.translateButton, on: self)
      }

      shouldShowTranslationOnboardingThisSession = false
    } else if Preferences.Translate.translateEnabled.value {
      completion(true)
    }
  }

  func presentToast(_ languageInfo: BraveTranslateLanguageInfo) {
    let popover = PopoverController(
      content: TranslateToast(languageInfo: languageInfo, presentSettings: { [weak self] in
        guard let self = self else { return }
        
        let popup = PopupViewController(rootView: TranslateSettingsView(), isDismissable: true)
        self.present(popup, animated: true)
      }, revertTranslation: { [weak self] in
        guard let self = self else { return }
        print("Dismissing")
      }),
      autoLayoutConfiguration: .phoneWidth
    )

    popover.popoverDidDismiss = { [weak self] _ in

    }
    popover.present(from: self.topToolbar.locationView.translateButton, on: self)
  }
}
