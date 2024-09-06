// Copyright (c) 2024 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// You can obtain one at https://mozilla.org/MPL/2.0/.
import * as React from 'react'
import { skipToken } from '@reduxjs/toolkit/query/react'

// Types
import {
  BraveWallet,
  SerializableTransactionInfo //
} from '../../../../constants/types'

// Utils
import { getLocale, splitStringForTag } from '$web-common/locale'
import {
  findTransactionToken,
  getFormattedTransactionTransferredValue,
  getIsTxApprovalUnlimited,
  getTransactionApprovalTargetAddress,
  getTransactionToAddress,
  isBridgeTransaction,
  isSwapTransaction
} from '../../../../utils/tx-utils'
import { getCoinFromTxDataUnion } from '../../../../utils/network-utils'
import { getAddressLabel } from '../../../../utils/account-utils'
import Amount from '../../../../utils/amount'

// Queries
import {
  useAccountQuery,
  useGetCombinedTokensListQuery
} from '../../../../common/slices/api.slice.extra'
import {
  useGetAccountInfosRegistryQuery,
  useGetNetworkQuery
} from '../../../../common/slices/api.slice'
import {
  accountInfoEntityAdaptorInitialState //
} from '../../../../common/slices/entities/account-info.entity'

// Hooks
import {
  useTransactionsNetwork //
} from '../../../../common/hooks/use-transactions-network'
import {
  useSwapTransactionParser //
} from '../../../../common/hooks/use-swap-tx-parser'
import { useExplorer } from '../../../../common/hooks/explorer'

// Styled Components
import { Button, ExplorerIcon } from './common.style'
import { Row, Text } from '../../../shared/style'

interface Props {
  transaction: SerializableTransactionInfo
}

export const TransactionIntent = (props: Props) => {
  const { transaction } = props

  // Computed
  const isBridge = isBridgeTransaction(transaction)
  const isSwap = isSwapTransaction(transaction)
  const isSwapOrBridge = isBridge || isSwap
  const isERC20Approval =
    transaction.txType === BraveWallet.TransactionType.ERC20Approve
  const isTxApprovalUnlimited = getIsTxApprovalUnlimited(transaction)
  const txApprovalTarget = getTransactionApprovalTargetAddress(transaction)
  const txCoinType = getCoinFromTxDataUnion(transaction.txDataUnion)
  const txToAddress = getTransactionToAddress(transaction)

  // Queries
  const { account: txAccount } = useAccountQuery(transaction.fromAccountId)

  // Custom Hooks
  const transactionNetwork = useTransactionsNetwork(transaction)
  const onClickViewOnBlockExplorer = useExplorer(transactionNetwork)

  const { normalizedTransferredValue } =
    getFormattedTransactionTransferredValue({
      tx: transaction,
      txAccount,
      txNetwork: transactionNetwork
    })

  const { data: txNetwork } = useGetNetworkQuery({
    chainId: transaction.chainId,
    coin: txCoinType
  })

  const { data: bridgeToNetwork } = useGetNetworkQuery(
    isBridge &&
      transaction.swapInfo?.toChainId &&
      transaction.swapInfo.toCoin !== undefined
      ? {
          chainId: transaction.swapInfo.toChainId,
          coin: transaction.swapInfo.toCoin
        }
      : skipToken
  )

  const { data: accountInfosRegistry = accountInfoEntityAdaptorInitialState } =
    useGetAccountInfosRegistryQuery(undefined)

  const { buyToken, sellToken, buyAmountWei, sellAmountWei } =
    useSwapTransactionParser(transaction)

  const { data: combinedTokensList } = useGetCombinedTokensListQuery()

  const formattedSellAmount = sellToken
    ? sellAmountWei
        .divideByDecimals(sellToken.decimals)
        .formatAsAsset(6, sellToken.symbol)
    : ''
  const formattedBuyAmount = buyToken
    ? buyAmountWei
        .divideByDecimals(buyToken.decimals)
        .formatAsAsset(6, buyToken.symbol)
    : ''

  const recipientLabel = getAddressLabel(
    isERC20Approval ? txApprovalTarget : txToAddress,
    accountInfosRegistry
  )

  const transactionsToken = findTransactionToken(
    transaction,
    combinedTokensList
  )

  const formattedSendAmount = transactionsToken
    ? transactionsToken.isErc721 ||
      transactionsToken.isErc1155 ||
      transactionsToken.isNft
      ? `${transactionsToken.name} ${transactionsToken.symbol}`
      : new Amount(normalizedTransferredValue).formatAsAsset(
          6,
          transactionsToken.symbol
        )
    : ''

  const formattedApprovalAmount = isTxApprovalUnlimited
    ? `${getLocale('braveWalletTransactionApproveUnlimited')} ${
        transactionsToken?.symbol ?? ''
      }`
    : formattedSendAmount

  const transactionFailed =
    transaction.txStatus === BraveWallet.TransactionStatus.Dropped ||
    transaction.txStatus === BraveWallet.TransactionStatus.Error

  const transactionConfirmed =
    transaction.txStatus === BraveWallet.TransactionStatus.Confirmed

  const firstDuringValue = isERC20Approval
    ? formattedApprovalAmount
    : isSwapOrBridge && transactionConfirmed
    ? formattedBuyAmount
    : isSwapOrBridge
    ? formattedSellAmount
    : formattedSendAmount

  const secondDuringValue =
    isSwapOrBridge && transactionConfirmed
      ? recipientLabel
      : isBridge
      ? bridgeToNetwork?.chainName ?? ''
      : isSwap
      ? formattedBuyAmount
      : recipientLabel

  const sendSwapOrBridgeLocale = getLocale(
    isBridge
      ? 'braveWalletBridge'
      : isSwap
      ? 'braveWalletSwap'
      : 'braveWalletSend'
  ).toLocaleLowerCase()

  const descriptionString = getLocale(
    transactionFailed
      ? 'braveWalletErrorAttemptingToTransact'
      : isSwapOrBridge && transactionConfirmed
      ? 'braveWalletAmountAddedToAccount'
      : isBridge
      ? 'braveWalletBridgingAmountToNetwork'
      : isSwap
      ? 'braveWalletSwappingAmountToAmountOnNetwork'
      : isERC20Approval
      ? 'braveWalletApprovingAmountOnExchange'
      : transactionConfirmed
      ? 'braveWalletAmountSentToAccount'
      : 'braveWalletSendingAmountToAccount'
  )
    .replace('$5', firstDuringValue)
    .replace('$6', secondDuringValue)
    .replace(
      '$7',
      transactionFailed ? sendSwapOrBridgeLocale : txNetwork?.chainName ?? ''
    )

  const {
    beforeTag: firstBefore,
    duringTag: firstDuring,
    afterTag: firstAfter
  } = splitStringForTag(descriptionString, 1)
  const {
    beforeTag: secondBefore,
    duringTag: secondDuring,
    afterTag: secondAfter
  } = splitStringForTag(firstAfter || '', 3)

  return (
    <Row
      gap='4px'
      flexWrap='wrap'
    >
      {firstBefore && (
        <Text
          textColor='secondary'
          textSize='14px'
          isBold={false}
        >
          {firstBefore}
        </Text>
      )}
      {firstDuring && (
        <Text
          textColor='primary'
          textSize='14px'
          isBold={true}
        >
          {firstDuring}
        </Text>
      )}
      {secondBefore && (
        <Text
          textColor='secondary'
          textSize='14px'
          isBold={false}
        >
          {secondBefore}
        </Text>
      )}
      {secondDuring && (
        <Row
          width='unset'
          gap='4px'
        >
          <Text
            textColor='primary'
            textSize='14px'
            isBold={true}
          >
            {secondDuring}
          </Text>
          <Button
            onClick={onClickViewOnBlockExplorer(
              isSwapOrBridge && transaction.swapInfo?.provider === 'lifi'
                ? 'lifi'
                : 'tx',
              transaction.txHash
            )}
          >
            <ExplorerIcon />
          </Button>
        </Row>
      )}
      {secondAfter && (
        <Text
          textColor='secondary'
          textSize='14px'
          isBold={false}
        >
          {secondAfter}
        </Text>
      )}
    </Row>
  )
}
