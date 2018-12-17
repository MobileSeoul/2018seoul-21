module.exports = ({init, db, logger, errorMessage}) => {
	const {checkLoggedIn, checkLoggedOut} = require('../middleware/authenticate')
	const Following = require('../model/following')
	const api = require('express').Router()

	api.get('/count', async (req, res) => {
		let userId = req.query.userId

		if (!userId || typeof(userId) !== 'number') {
			return res.status(400).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (userId)'
			})
		}

		try {
			const query = {user: userId}
			let count = await Following.count(query)

			res.status(200).json({ data: count })
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	api.get('/followers/count', async (req, res) => {
		let userId = req.query.userId

		if (!userId || typeof(userId) !== 'number') {
			return res.status(404).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (userId)'
			})
		}

		try {
			const query = {followUser: userId} //팔로우 유저가 자기자신인 것
			let count = await Following.count(query)

			res.status(200).json({ data: count })
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	//자신이 팔로우하고있는 유저 정보 가져옴
	api.get('/', async (req, res) => {
		let userId = req.query.userId
		let skip = req.query.skip
		let limit = req.query.limit

		if (!userId || typeof(userId) !== 'number') {
			return res.status(404).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (userId)'
			})
		}

		if (!skip || typeof(skip) !== 'number') {
			skip = 0
		}

		if (!limit || typeof(limit) !== 'number') {
			limit = 30
		}

		let users = []
		const query = {user: userId} //유저가 자기자신인 것
		const fields = 'followUser'
		const options = { skip: skip, limit: limit, sort: 'date' }

		try {
			let followings = await Following.find(query, fields, options)
			.populate('followUser')

			followings.forEach(following => {
				users.append(following.followUser)
			})

			res.status(200).json({ data: users })
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	//자신을 팔로우 하고있는 유저 정보를 가져옴
	api.get('/followers', async (req, res) => {
		let userId = req.query.userId
		let skip = req.query.skip
		let limit = req.query.limit

		if (!userId || typeof(userId) !== 'number') {
			return res.status(404).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (userId)'
			})
		}

		if (!skip || typeof(skip) !== 'number') {
			skip = 0
		}
		if (!limit || typeof(limit) !== 'number') {
			limit = 30
		}

		const query = {followUser: userId}
		const fields = 'user'
		const options = { skip: skip, limit: limit, sort: 'date' }

		try {
			let users = await Following.find(query, fields, options)
			.populate('user')

			res.status(200).json({ data: users })
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	//팔로우 추가
	api.post('/', checkLoggedIn, async (req, res) => {
		let userId = req.user._id
		let followUserId = req.query.followUserId

		if (!followUserId || typeof(followUserId) !== 'number') {
			return res.status(404).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (followUserId)'
			})
		}

		try {
			let following = await Following.create({
				user: userId,
				followUser: followUserId
			})

			res.status(201).json({ data: following })
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	api.delete('/', checkLoggedIn, async (req, res) => {
		let userId = req.user._id
		let followUserId = req.query.followUserId

		if (!followUserId || typeof(followUserId) !== 'number') {
			return res.status(404).json({
				message: errorMessage.INVALID_QUERY_PARAMETER + ' (followUserId)'
			})
		}

		try {
			const query = {user: userId, followUser: followUserId}
			await Following.findOneAndDelete(query)

			res.status(200).json({})
		} catch (err) {
			logger.error(err.message)
			res.status(500).json({ message: err.message })
		}
	})

	return api
}