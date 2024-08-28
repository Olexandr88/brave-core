// Copyright (c) 2024 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// You can obtain one at https://mozilla.org/MPL/2.0/.

import * as React from 'react'
import Checkbox from '@brave/leo/react/checkbox'
import Button from '@brave/leo/react/button'

// Utils
import { getLocale, splitStringForTag } from '../../../../../common/locale'

// Selectors
import { useSafeUISelector } from '../../../../common/hooks/use-safe-selector'
import { UISelectors } from '../../../../common/selectors'

// Assets
import PageTermsGraphic from './assets/page_terms_graphic.svg'
import PanelTermsGraphic from './assets/panel_terms_graphic.svg'

// Styled Components
import {
  TermsText,
  TermsDialog,
  Title,
  TermsLabel
} from './partners_consent_modal.style'
import { Row } from '../../../shared/style'

interface PartnerConsentModalProps {
  isOpen: boolean
  onClose: () => void
  onContinue: () => void
  onCancel: () => void
}

export function PartnersConsentModal({
  isOpen,
  onClose,
  onContinue,
  onCancel
}: PartnerConsentModalProps) {
  // state
  const [termsAccepted, setTermsAccepted] = React.useState(false)

  // redux
  const isPanel = useSafeUISelector(UISelectors.isPanel)

  const { beforeTag, duringTag } = splitStringForTag(
    getLocale('braveWalletMeldTermsOfUse'),
    1
  )

  return (
    <TermsDialog
      isOpen={isOpen}
      backdropClickCloses={false}
      escapeCloses={false}
      onClose={onClose}
      showClose
    >
      <Title slot='title'>{getLocale('braveWalletTransactionsPartner')}</Title>
      <Row justifyContent='center'>
        <img
          src={isPanel ? PanelTermsGraphic : PageTermsGraphic}
          alt='Terms Graphic'
        />
      </Row>
      <TermsText>{getLocale('braveWalletTransactionPartnerConsent')}</TermsText>
      <Checkbox
        checked={termsAccepted}
        onChange={(e) => setTermsAccepted(e.checked)}
      >
        <TermsLabel>
          {beforeTag}
          <a
            target='_blank'
            href='https://www.meld.io/terms-of-use'
          >
            {duringTag}
          </a>
        </TermsLabel>
      </Checkbox>
      <Row
        justifyContent='space-between'
        gap='16px'
        padding='24px 0 0'
      >
        <Button
          kind='outline'
          onClick={onCancel}
        >
          {getLocale('braveWalletButtonCancel')}
        </Button>
        <Button
          isDisabled={!termsAccepted}
          onClick={onContinue}
        >
          {getLocale('braveWalletButtonContinue')}
        </Button>
      </Row>
    </TermsDialog>
  )
}
