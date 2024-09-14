/* Copyright (c) 2022 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

namespace x {

	const supportedPaths = [
		"/",
		"/home",
		"/notifications"
	];

	const isString = (value: any) => typeof value === "string";
	const isSupportedPath = supportedPaths.includes(window.location.pathname);

	class XStateStore {

		private snapshot: Record<string, any> | null;

		constructor() {
			const reactRoot = document.querySelector("#react-root");
			const storeHost = reactRoot?.firstElementChild?.firstElementChild;
			if (storeHost instanceof HTMLElement) {
				const reduxStore = getXProps(storeHost)?.children?.props?.store ?? {};
				this.snapshot = (reduxStore.getState instanceof Function) ? reduxStore.getState() : null;
			}
		}

		access(type: string, id: string) {
			type = type === "notifications" ? "genericNotifications" : type;
			return this.snapshot?.entities?.[type]?.entities?.[id] ?? null;
		}

	}

	/**
	 * Retrieves React-specific properties from an element by
	 * finding the first property starting with "__reactProps"
	 */
	function getXProps(element: HTMLElement | null) {
		if (element instanceof HTMLElement) {
			for (const property in element) {
				if (property.startsWith("__reactProps")) {
					return element[property];
				}
			}
		}
		return null;
	}

	/**
	 * A class to manage the snapshot of the Redux store for
	 * accessing entities (like tweets or notifications) with
	 * a method to handle access based on entity type.
	 */
	const store = new XStateStore();

	/**
	 * Decodes HTML entities in a given string using a
	 * `textarea` element.
	 */
	function decodeHTMLSpecialChars(text: string) {
		const textarea = document.createElement("textarea");
		textarea.innerHTML = text;
		return textarea.value;
	}

	const config = {
		"tweet": {
			"selector": "[data-testid='tweet']",
			"distiller": distillPostElement
		},
		"notification": {
			"selector": "[data-testid='notification']",
			"distiller": distillNotificationElement,
		}
	};

	export function distill() {
		if (!isSupportedPath) {
			return null;
		}
		return distillPrimaryColumn();
	}

	/**
	 * Extracts and processes all items (like tweets and
	 * notifications) from the primary column of the page, using
	 * configuration to determine which elements to distill.
	 */
	function distillPrimaryColumn() {
		const primaryColumn = document.querySelector("[data-testid='primaryColumn']")
		const itemSelectors = `${config.tweet.selector}, ${config.notification.selector}`;
		const timelineItems = primaryColumn?.querySelectorAll(itemSelectors) ?? [];

		return Array.from(timelineItems).map(item => {
			const type = item.getAttribute("data-testid");
			const { distiller } = type && config[type];
			return distiller && distiller(item);
		}).filter(Boolean).join("\n\n---\n\n");
	}

	/**
	 * Generates a list of formatted metrics (like Likes,
	 * Quotes, Replies) for a given tweet, considering the
	 * plurality of values.
	 * 
	 * TODO (Sampson): Come back and define some types, such
	 * as the `post` parameter.
	 */
	const getMetrics = (post: any) => {
		const metrics = [
			{ key: "favorite", singular: "Like", plural: "Likes" },
			{ key: "quote", singular: "Quote", plural: "Quotes" },
			{ key: "reply", singular: "Reply", plural: "Replies" },
			{ key: "retweet", singular: "Repost", plural: "Reposts" },
			{ key: "bookmark", singular: "Bookmark", plural: "Bookmarks" },
		];

		return metrics
			.map(({ key, singular, plural }) => {
				const value = post[`${key}_count`];
				if (value === undefined) return null;
				const label = value === 1 ? singular : plural;
				return `${value.toLocaleString()} ${label}`;
			})
			.filter(Boolean);
	}

	/**
	 * Extracts post data from a DOM element and distills
	 * it into a formatted output.
	 */
	function distillPostElement(element: HTMLElement) {
		const tweetId = getXProps(element.parentElement)?.children?.props?.entry?.content?.id;
		const postData = tweetId && store.access("tweets", tweetId);

		return postData ? distillPostData(postData) : element.innerText;
	}

	function distillNotificationElement(element: HTMLElement) {
		const notificationId = getXProps(element.parentElement)?.children?.props?.entry?.content?.id;
		const notificationData = store.access("notifications", notificationId);

		if (notificationData) {
			const distilled = distillNotificationData(notificationData);
			if (distilled) {
				return distilled;
			}
		}

		return element.innerText;
	}

	function distillNotificationData(notification: any) {
		// TODO (Sampson): Implement this method to distill a notification.
		return '';
	}

	/**
	 * Constructs the text output for a given post, handling
	 * reposts, headers, and post body content.
	 */
	function distillPostData(post: any, level: number = 0): string | null {
		if (post.retweeted_status) {
			return distillRepost(post);
		}

		const header = distillPostHeader(post);
		const body = buildPostBody(post, level);

		return [header, body && indentLines(`\n${body}`)]
			.filter(Boolean).join('\n');
	}

	/**
	 * Builds the main content of a post, including
	 * handling quoted content and text wrapping.
	 */
	function buildPostBody(post: any, level: number) {
		const text = getPostText(post);
		const quoted = post.quoted_status ? buildQuotedBody(post, level) : null;
		const card = post.card ? distillCard(post) : null;

		return [
			text ? wrapLines(text) : null,
			card ? [`\n${indentLines(card, ' > ')}`] : null,
			quoted ? [`\n${indentLines(quoted, ' > ')}`] : null
		].flat().filter(Boolean).join('\n');
	}

	function distillCard(post: any) {
		const card = store.access("cards", post.card);
		if (card) {
			const { binding_values: { title, description, url } } = card;
			return [
				title ? `Title: ${title}` : null,
				description ? `Description: ${description}` : null,
				url ? `URL: ${url}` : null
			].filter(Boolean).join('\n');
		}
		return null;
	}

	/**
	 * Creates the header of a post, including the author
	 * information, action line, and any relevant metrics.
	 */
	function distillPostHeader(post: any) {
		const author = store.access("users", post.user);
		const signature = getUserSignature(author);
		const metrics = getMetrics(post);

		return [
			`From: ${signature}`,
			buildActionLine(post),
			metrics.length > 0 ? `Metrics: ${metrics.join(" | ")}` : null
		].filter(Boolean).join('\n');
	}

	/**
	 * Retrieves and decodes the main text of a post, handling
	 * different fields where the text might be located.
	 */
	const getPostText = (post: any) => {
		const response = isString(post.note_tweet?.text) ? post.note_tweet.text : post.full_text;
		return isString(response) ? decodeHTMLSpecialChars(response) : null;
	}

	/**
	 * Formats a reposted tweet, incorporating the original
	 * tweet’s content and indicating who reposted it.
	 */
	function distillRepost(post: any): string | null {
		const repostingUser = store.access("users", post.user);
		const repostingPost = store.access("tweets", post.retweeted_status);
		if (repostingUser && repostingPost) {
			const distilled = distillPostData(repostingPost);
			if (distilled) {
				const [fromUser, ...rest] = distilled.split('\n');
				const repostedBy = `Reposted by ${getUserSignature(repostingUser)}`;
				return [`${fromUser} (${repostedBy})`, ...rest].join('\n');
			}
		}
		return null;
	}

	/**
	 * Generates a formatted signature string for a user,
	 * considering different possible representations (name,
	 * screen name, or ID).
	 */
	const getUserSignature = (user: any) => {
		if (isString(user) || Number.isInteger(user))
			user = store.access("users", user);

		const { name, screen_name: screenName, id_str: idStr } = user ?? {};

		if (isString(screenName))
			return isString(name)
				? `${name} <@${screenName}>`
				: `@${screenName}`;

		return idStr ? `User #${idStr}` : "Unknown User";
	}

	/**
	 * Constructs an action line for a post, such as indicating
	 * a reply or when the post was created.
	 */
	const buildActionLine = (post: any) => {
		const date = new Date(post.created_at).toLocaleString();
		if (post.in_reply_to_user_id_str && post.in_reply_to_screen_name) {
			const user = store.access("users", post.in_reply_to_user_id_str);
			const signature = getUserSignature(user) ?? `@${post.in_reply_to_screen_name}`;
			return `Replied to ${signature}: ${date}`;
		}
		return `Posted: ${date}`;
	}

	/**
	 * Handles the content for quoted posts, including truncation
	 * logic and URL continuation if the maximum depth is reached.
	 */
	const buildQuotedBody = (post: any, level: number = 0): string | null => {
		const quote = store.access("tweets", post.quoted_status);
		const distilled = level < 3 && quote ? distillPostData(quote, level + 1) : null;
		return distilled || `Thread continues at ${getExpandedURL(post)}`;
	}

	/**
	 * Retrieves the expanded URL for a post, either from the
	 * provided permalink or by constructing it using user
	 * information.
	 */
	function getExpandedURL(post: any) {
		const url = post.quoted_status_permalink?.expandedUrl;
		if (url) return url;
		const { screen_name: screenName } = store.access("users", post.user) ?? {};
		return isString(screenName) ? `https://x.com/${screenName}/status/${post.id_str}` : null;
	}

	/**
	 * Indents each line of the given text by a specified
	 * number of spaces or a string prefix.
	 */
	const indentLines = (text: string, indent: number | string = 2) => {
		const prefix = Number.isInteger(indent) ? ' '.repeat(indent as number) : indent;
		return text.split('\n').map(line => `${prefix}${line}`).join('\n');
	}

	/**
	 * Wraps lines of text to a specified maximum length,
	 * breaking at spaces to ensure readability.
	 */
	function wrapLines(text: string, limit: number = 80) {
		return text.split('\n').map(line => {
			const words = line.split(' ');
			let replace = '';
			let length = 0;
			for (const word of words) {
				if (length + word.length > limit) {
					replace += '\n';
					length = 0;
				}
				replace += word + ' ';
				length += word.length + 1;
			}
			return replace.trim();
		}).join('\n');
	}

}

// @ts-expect-error
// This file will be wrapped in an IIFE by Webpack
return x.distill();