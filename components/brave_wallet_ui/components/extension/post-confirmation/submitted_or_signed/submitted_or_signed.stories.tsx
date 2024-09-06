// Copyright (c) 2022 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// you can obtain one at https://mozilla.org/MPL/2.0/.
import * as React from 'react'

// Types
import {
  BraveWallet,
  StorybookTransactionArgs,
  StorybookTransactionOptions
} from '../../../../constants/types'

// Mocks
import {
  getPostConfirmationStatusMockTransaction //
} from '../../../../stories/mock-data/mock-transaction-info'

// Components
import {
  WalletPanelStory //
} from '../../../../stories/wrappers/wallet-panel-story-wrapper'
import { TransactionSubmittedOrSigned } from './submitted_or_signed'
import { CancelTransaction } from '../cancel_transaction/cancel_transaction'

// Styled Components
import { LongWrapper } from '../../../../stories/style'
import { PanelWrapper } from '../../../../panel/style'

export const _TransactionSubmittedOrSigned = {
  render: (args: StorybookTransactionArgs) => {
    // Props
    const { transactionType } = args

    // State
    const [showCancelTransaction, setShowCancelTransaction] =
      React.useState(false)

    // Computed
    const transaction = getPostConfirmationStatusMockTransaction(
      transactionType,
      BraveWallet.TransactionStatus.Submitted
    )

    return (
      <WalletPanelStory>
        <PanelWrapper
          width={390}
          height={650}
        >
          <LongWrapper padding='0px'>
            {showCancelTransaction ? (
              <CancelTransaction
                onBack={() => setShowCancelTransaction(false)}
                transaction={transaction}
              />
            ) : (
              <TransactionSubmittedOrSigned
                onClose={() => alert('Close panel screen clicked.')}
                onShowCancelTransaction={() => setShowCancelTransaction(true)}
                transaction={transaction}
                onClickViewInActivity={() => alert('View in activity clicked.')}
              />
            )}
          </LongWrapper>
        </PanelWrapper>
      </WalletPanelStory>
    )
  }
}

export default {
  component: TransactionSubmittedOrSigned,
  argTypes: {
    transactionType: {
      options: StorybookTransactionOptions,
      control: { type: 'select' }
    }
  }
}
