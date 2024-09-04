/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

import * as React from 'react'

import { AppModelContext, useAppState } from '../../lib/app_model_context'
import { useCallbackWrapper } from '../../lib/callback_wrapper'
import { isSelfCustodyProvider } from '../../../shared/lib/external_wallet'
import { Modal } from '../modal'
import { PaymentForm } from './payment_form'
import { PaymentSelection } from './payment_selection'
import { Sending } from './sending'
import { Success } from './success'
import { Error } from './error'

import { style } from './contribute_modal.style'

type ViewType =
  'payment-selection' |
  'payment-form' |
  'sending' |
  'success' |
  'error'

interface Props {
  onClose: () => void
}

export function ContributeModal (props: Props) {
  const model = React.useContext(AppModelContext)
  const wrapCallback = useCallbackWrapper()

  const [creator, externalWallet] = useAppState((state) => [
    state.currentCreator,
    state.externalWallet
  ])

  const [viewType, setViewType] = React.useState<ViewType>(() => {
    if (creator && externalWallet) {
      const hasMatchingCustodialProvider =
        creator.supportedWalletProviders.includes(externalWallet.provider) &&
        !isSelfCustodyProvider(externalWallet.provider)

      if (hasMatchingCustodialProvider && !creator.banner.web3URL) {
        return 'payment-form'
      }
    }
    return 'payment-selection'
  })

  function onSend(amount: number, recurring: boolean) {
    if (!creator) {
      return
    }

    setViewType('sending')

    model
      .sendContribution(creator.site.id, amount, recurring)
      .then(wrapCallback((success) => {
        setViewType(success ? 'success' : 'error')
      }))
  }

  function closeDisabled() {
    if (viewType === 'sending') {
      return true
    }
    return false
  }

  function headerTitle() {
    switch (viewType) {
      case 'sending':
      case 'success':
      case 'error':
        return ''
    }
    return 'Contribute'
  }

  function renderContent() {
    switch (viewType) {
      case 'payment-form':
        return <PaymentForm onCancel={props.onClose} onSend={onSend} />
      case 'payment-selection':
        return (
          <PaymentSelection
            onSelectCustodial={() => setViewType('payment-form')}
            onCancel={props.onClose}
          />
        )
      case 'sending':
        return <Sending />
      case 'success':
        return <Success />
      case 'error':
        return <Error />
    }
  }

  return (
    <Modal onEscape={props.onClose}>
      <Modal.Header
        title={headerTitle()}
        onClose={props.onClose}
        closeDisabled={closeDisabled()}
      />
      <div {...style}>
        {renderContent()}
      </div>
    </Modal>
  )
}
