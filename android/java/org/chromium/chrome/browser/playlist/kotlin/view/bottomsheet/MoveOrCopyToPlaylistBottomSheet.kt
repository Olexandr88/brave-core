/*
 * Copyright (c) 2023 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.chromium.chrome.browser.playlist.kotlin.view.bottomsheet

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.chromium.chrome.browser.playlist.kotlin.PlaylistViewModel
import org.chromium.chrome.R
import org.chromium.chrome.browser.playlist.kotlin.adapter.recyclerview.PlaylistAdapter
import org.chromium.chrome.browser.playlist.kotlin.enums.PlaylistOptionsEnum
import org.chromium.chrome.browser.playlist.kotlin.extension.setTopCornersRounded
import org.chromium.chrome.browser.playlist.kotlin.fragment.AllPlaylistFragment
import org.chromium.chrome.browser.playlist.kotlin.fragment.NewPlaylistFragment
import org.chromium.chrome.browser.playlist.kotlin.listener.PlaylistClickListener
import org.chromium.chrome.browser.playlist.kotlin.model.MoveOrCopyModel
import org.chromium.chrome.browser.playlist.kotlin.model.PlaylistModel
import org.chromium.chrome.browser.playlist.kotlin.util.ConstantUtils
import org.chromium.chrome.browser.playlist.kotlin.util.PlaylistUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import org.chromium.chrome.browser.playlist.kotlin.activity.PlaylistBaseActivity
import org.chromium.chrome.browser.playlist.kotlin.activity.NewPlaylistActivity
import org.chromium.playlist.mojom.PlaylistService
import org.chromium.playlist.mojom.Playlist

class MoveOrCopyToPlaylistBottomSheet :
    BottomSheetDialogFragment(), PlaylistClickListener {

    private lateinit var mPlaylistViewModel: PlaylistViewModel
    private val mMoveOrCopyModel: MoveOrCopyModel = PlaylistUtils.moveOrCopyModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_add_or_move_to_playlist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPlaylistViewModel = ViewModelProvider(requireActivity())[PlaylistViewModel::class.java]

        val layoutBottomSheet: MaterialCardView = view.findViewById(R.id.layoutBottomSheet)
        layoutBottomSheet.setTopCornersRounded(16)

        var fromPlaylistId = ""
        if (mMoveOrCopyModel.playlistItems.isNotEmpty()) {
            fromPlaylistId = mMoveOrCopyModel.fromPlaylistId
        }

        getPlaylistService()?.getAllPlaylists {
            playlists -> 
            val allPlaylists = mutableListOf<Playlist>()
            for (playlist in playlists) {
                if (playlist.id != fromPlaylistId) {
                    allPlaylists.add(playlist)
                }
            }

            val playlist = Playlist()
            playlist.id = ConstantUtils.NEW_PLAYLIST
            playlist.name = getString(R.string.playlist_new_text)
            playlist.items = emptyArray()

            allPlaylists.add(
                0,
                playlist
            )

            val rvPlaylists: RecyclerView = view.findViewById(R.id.rvPlaylists)
            rvPlaylists.layoutManager = LinearLayoutManager(view.context)
            val playlistAdapter = PlaylistAdapter(this)
            rvPlaylists.adapter = playlistAdapter
            playlistAdapter.submitList(allPlaylists)
        }


        // mPlaylistViewModel.fetchPlaylistData(ConstantUtils.ALL_PLAYLIST)

        // mPlaylistViewModel.allPlaylistData.observe(viewLifecycleOwner) { allPlaylistData ->
        //     val allPlaylistList = mutableListOf<PlaylistModel>()
        //     for (allPlaylistModel in allPlaylistData) {
        //         if (allPlaylistModel.id != fromPlaylistId) {
        //             allPlaylistList.add(
        //                 PlaylistModel(
        //                     allPlaylistModel.id,
        //                     allPlaylistModel.name,
        //                     allPlaylistModel.items
        //                 )
        //             )
        //         }
        //     }

        //     allPlaylistList.add(
        //         0,
        //         PlaylistModel(
        //             ConstantUtils.NEW_PLAYLIST,
        //             getString(R.string.playlist_new_text),
        //             arrayListOf()
        //         )
        //     )

        //     val rvPlaylists: RecyclerView = view.findViewById(R.id.rvPlaylists)
        //     rvPlaylists.layoutManager = LinearLayoutManager(view.context)
        //     val playlistAdapter = PlaylistAdapter(this)
        //     rvPlaylists.adapter = playlistAdapter
        //     // playlistAdapter.submitList(allPlaylistList)
        // }

        val behavior = BottomSheetBehavior.from(layoutBottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
    }

    override fun onPlaylistClick(playlist: Playlist) {
        if (playlist.id == ConstantUtils.NEW_PLAYLIST) {
            PlaylistUtils.moveOrCopyModel =
                MoveOrCopyModel(
                    mMoveOrCopyModel.playlistOptionsEnum,
                    mMoveOrCopyModel.fromPlaylistId,
                    "",
                    mMoveOrCopyModel.playlistItems
                )
            val newActivityIntent = Intent(requireActivity(), NewPlaylistActivity::class.java);
            startActivity(newActivityIntent);
            // val newPlaylistFragment = NewPlaylistFragment.newInstance(
            //     PlaylistOptionsEnum.NEW_PLAYLIST,
            //     shouldMoveOrCopy = true
            // )
            // parentFragmentManager
            //     .beginTransaction()
            //     .replace(android.R.id.content, newPlaylistFragment)
            //     .addToBackStack(AllPlaylistFragment::class.simpleName)
            //     .commit()
        } else {
            PlaylistUtils.moveOrCopyModel = MoveOrCopyModel(
                mMoveOrCopyModel.playlistOptionsEnum,
                mMoveOrCopyModel.fromPlaylistId,
                playlist.id,
                mMoveOrCopyModel.playlistItems
            )
            if (PlaylistUtils.moveOrCopyModel.playlistOptionsEnum
                                            == PlaylistOptionsEnum.MOVE_PLAYLIST_ITEM
                                    || PlaylistUtils.moveOrCopyModel.playlistOptionsEnum
                                            == PlaylistOptionsEnum.MOVE_PLAYLIST_ITEMS) {
                PlaylistUtils.moveOrCopyModel.playlistItems.forEach {
                    getPlaylistService()?.moveItem(PlaylistUtils.moveOrCopyModel.fromPlaylistId,
                                            PlaylistUtils.moveOrCopyModel.toPlaylistId,
                                            it.id);
                }
            } else {
                val playlistItemIds = Array<String>(PlaylistUtils.moveOrCopyModel.playlistItems.size) { "" }
                for (i in PlaylistUtils.moveOrCopyModel.playlistItems.indices) {
                    playlistItemIds[i] = PlaylistUtils.moveOrCopyModel.playlistItems[i].id
                }
                getPlaylistService()?.copyItemToPlaylist(playlistItemIds, PlaylistUtils.moveOrCopyModel.toPlaylistId)
            }
        }
        dismiss()
    }

    private fun getPlaylistService() : PlaylistService? {
        val activity: Activity? = activity
        return if (activity is PlaylistBaseActivity) {
            (activity as PlaylistBaseActivity?)?.getPlaylistService()
        } else null
    }
}
