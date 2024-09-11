/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_ads/core/internal/account/user_data/fixed/page_land_user_data.h"

#include "base/test/values_test_util.h"
#include "brave/components/brave_ads/core/internal/common/test/test_base.h"
#include "brave/components/brave_ads/core/internal/settings/settings_test_util.h"
#include "brave/components/brave_ads/core/internal/tabs/tab_info.h"
#include "net/http/http_status_code.h"
#include "url/gurl.h"

// npm run test -- brave_unit_tests --filter=BraveAds*

namespace brave_ads {

class BraveAdsPageLandUserDataTest : public test::TestBase {};

TEST_F(BraveAdsPageLandUserDataTest,
       BuildPageLandUserDataForHttpInformationalResponseStatusCodeClass) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(
      TabInfo{/*id=*/1,
              /*is_visible=*/true,
              /*redirect_chain=*/{GURL("https://brave.com")},
              net::HTTP_SWITCHING_PROTOCOLS,
              /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "1xx"
                    })"),
            user_data);
}

TEST_F(BraveAdsPageLandUserDataTest,
       BuildPageLandUserDataForHttpSuccessfulResponseStatusCodeClass) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(
      TabInfo{/*id=*/1,
              /*is_visible=*/true,
              /*redirect_chain=*/{GURL("https://brave.com")}, net::HTTP_IM_USED,
              /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "2xx"
                    })"),
            user_data);
}

TEST_F(BraveAdsPageLandUserDataTest,
       BuildPageLandUserDataForHttpRedirectionMessageStatusCodeClass) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(
      TabInfo{/*id=*/1,
              /*is_visible=*/true,
              /*redirect_chain=*/{GURL("https://brave.com")},
              net::HTTP_MOVED_PERMANENTLY,
              /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "3xx"
                    })"),
            user_data);
}

TEST_F(BraveAdsPageLandUserDataTest,
       BuildPageLandUserDataForHttpClientErrorResponseStatusCode) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(TabInfo{
      /*id=*/1,
      /*is_visible=*/true,
      /*redirect_chain=*/{GURL("https://brave.com")}, net::HTTP_NOT_FOUND,
      /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "404"
                    })"),
            user_data);
}

TEST_F(BraveAdsPageLandUserDataTest,
       BuildPageLandUserDataForHttpClientErrorResponseStatusCodeClass) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(
      TabInfo{/*id=*/1,
              /*is_visible=*/true,
              /*redirect_chain=*/{GURL("https://brave.com")},
              net::HTTP_UPGRADE_REQUIRED,
              /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "4xx"
                    })"),
            user_data);
}

TEST_F(
    BraveAdsPageLandUserDataTest,
    BuildPageLandUserDataForPrivacyPreservingHttpServerErrorResponseStatusCode) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(
      TabInfo{/*id=*/1,
              /*is_visible=*/true,
              /*redirect_chain=*/{GURL("https://brave.com")},
              net::HTTP_INTERNAL_SERVER_ERROR,
              /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "500"
                    })"),
            user_data);
}

TEST_F(
    BraveAdsPageLandUserDataTest,
    BuildPageLandUserDataForPrivacyPreservingHttpServerErrorResponseStatusCodeClass) {
  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(TabInfo{
      /*id=*/1,
      /*is_visible=*/true,
      /*redirect_chain=*/{GURL("https://brave.com")}, net::HTTP_LOOP_DETECTED,
      /*is_playing_media=*/false});

  // Assert
  EXPECT_EQ(base::test::ParseJsonDict(
                R"(
                    {
                      "httpResponseStatus": "5xx"
                    })"),
            user_data);
}

TEST_F(
    BraveAdsPageLandUserDataTest,
    DoNotBuildPageLandUserDataForHttpResponseStatusErrorPageForNonRewardsUser) {
  // Arrange
  test::DisableBraveRewards();

  // Act
  const base::Value::Dict user_data = BuildPageLandUserData(TabInfo{
      /*id=*/1,
      /*is_visible=*/true,
      /*redirect_chain=*/{GURL("https://brave.com")}, net::HTTP_NOT_FOUND,
      /*is_playing_media=*/false});

  // Assert
  EXPECT_THAT(user_data, ::testing::IsEmpty());
}

}  // namespace brave_ads
