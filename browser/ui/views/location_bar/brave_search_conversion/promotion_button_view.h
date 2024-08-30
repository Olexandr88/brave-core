/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_BROWSER_UI_VIEWS_LOCATION_BAR_BRAVE_SEARCH_CONVERSION_PROMOTION_BUTTON_VIEW_H_
#define BRAVE_BROWSER_UI_VIEWS_LOCATION_BAR_BRAVE_SEARCH_CONVERSION_PROMOTION_BUTTON_VIEW_H_

#include <memory>

#include "base/functional/callback_forward.h"
#include "base/memory/raw_ptr.h"
#include "brave/browser/ui/views/view_shadow.h"
#include "ui/base/metadata/metadata_header_macros.h"
#include "ui/views/controls/button/button.h"

class ViewShadow;

namespace gfx {
class Image;
class SlideAnimation;
}  // namespace gfx

namespace views {
class ImageView;
}  // namespace views

class PromotionButtonView : public views::Button {
  METADATA_HEADER(PromotionButtonView, views::Button)

 public:
  PromotionButtonView();
  ~PromotionButtonView() override;

  void SetDismissedCallback(base::OnceClosure callback);
  void SetMakeDefaultCallback(base::RepeatingClosure callback);
  void UpdateTargetProviderImage(const gfx::Image& image);
  void AnimateExpand();

 private:
  // views::Button overrides:
  void StateChanged(views::Button::ButtonState old_state) override;
  void OnThemeChanged() override;
  gfx::Size CalculatePreferredSize(
      const views::SizeBounds& available_size) const override;
  ui::Cursor GetCursor(const ui::MouseEvent& event) override;
  void AnimationProgressed(const gfx::Animation* animation) override;

  void Update();

  float GetCornerRadius() const;
  void UpdateBackgroundAndBorders();
  void AddChildViews();
  void SetupShadow();
  void UpdateShadow();

  void OnButtonPressed();
  void OnClosePressed();

  raw_ptr<views::ImageView> target_provider_image_ = nullptr;
  std::unique_ptr<ViewShadow> shadow1_;
  std::unique_ptr<ViewShadow> shadow2_;
  std::unique_ptr<gfx::SlideAnimation> animation_;

  // Dismissed callback is only called once because after dismissed,
  // this button will not be shown forever.
  base::OnceClosure dismissed_callback_;

  // This button could be called multiple times because this promotion button
  // is shown again if user changed to another search provider.
  base::RepeatingClosure make_default_callback_;
};

#endif  // BRAVE_BROWSER_UI_VIEWS_LOCATION_BAR_BRAVE_SEARCH_CONVERSION_PROMOTION_BUTTON_VIEW_H_
