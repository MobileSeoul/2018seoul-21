module.exports = ({
	db,
	init,
	logger,
	check,
	errorMessage
}) => {
	const {
		checkLoggedIn
	} = require('../middleware/authenticate')
	const api = require('express').Router()
	const Comment = require('../model/comment')
	const {
		checkEmojiAndMemo,
		checkDuplicatedMemoAndUserid,
		checkSkipAndLimit,
		checkIdParams,
		checkEmoji
	} = require('../middleware/typeCheck')({
		logger,
		init
	})

	//@desc : 특정 메모에 속해있는 이모찌와 유저를 전부 가져옴
	//@router : POST http://localhost:3001/api/comments/:id
	//@params : id: String
	//@query : skip: String, limit: String
	api.get('/:id', [checkIdParams, checkSkipAndLimit], async (req, res) => {
		let memo = req.params.id
		let skip = req.query.skip
		let limit = req.query.limit

		try {
			let comments = await Comment.find({})
				.skip(parseInt(skip))
				.where('memo')
				.equals(memo)
				.select('user emoji')
				.populate('user')
				.limit(parseInt(limit))
				.sort('date')

			return res.status(200).json({
				data: comments
			})

		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 코멘트 추가
	//@router : POST http://localhost:3001/api/comments
	//@body : id: String, emoji: String, memo: String
	api.post('/', [checkLoggedIn, checkEmojiAndMemo, checkDuplicatedMemoAndUserid], async (req, res) => {
		let myUserId = req.user._id
		let memo = req.body.memo
		let emoji = req.body.emoji

		try {
			//코멘트 중복확인
			let countQuery = { user: myUserId, memo: memo }
			let count = await Comment.count(countQuery)

			if (count > 0) {
				return res.status(409).json({
					message: errorMessage.CONFLICT_COMMENT
				})
			}

			let query = {
				user: myUserId,
				memo: memo,
				emoji: emoji
			}
			let comment = await Comment.create(query)

			return res.status(201).json({
				data: comment
			})

		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 특정 메모의 자신의 코멘트 삭제
	//@router: DELETE http://localhost:3001/api/comments/:id
	//@params : id: String
	api.delete('/:id', [checkLoggedIn, checkIdParams], async (req, res) => {
		let myUserId = req.user._id
		let id = req.params.id

		try {
			const filter = {
				user: myUserId,
				memo: id
			}
			await Comment.findOneAndDelete(filter)

			res.status(200).json({})
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 특정 메모의 자신의 특정 코멘트 수정
	//@router: PUT http://localhost:3001/api/comments/:id
	//@params : id: String
	//@body : emoji: String
	api.put('/:id', [checkLoggedIn, checkIdParams, checkEmoji], async (req, res) => {
		let myUserId = req.user._id
		let memo = req.params.id
		let emoji = req.body.emoji

		let filter = {
			memo: memo,
			user: myUserId
		}
		let update = {
			$set: {
				emoji: emoji,
				date: Date.now()
			}
		}
		let option = {
			new: true,
			upsert: false
		}

		try {
			let comment = await Comment.findOneAndUpdate(filter, update, option)

			return res.status(200).json({
				data: comment
			})

		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	return api;
}