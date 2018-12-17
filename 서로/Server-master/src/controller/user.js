module.exports = ({
	init,
	db,
	logger,
	check
}) => {
	const passport = require('passport')
  	require('passport-local').Strategy;
	const {
		checkLoggedIn
	} = require('../middleware/authenticate')
	const User = require('../model/user')
	const Following = require('../model/following')
	const Memo = require('../model/memo')
	const api = require('express').Router()
	const hasher = require('pbkdf2-password')()
	const {
		checkRegisterUser,
		checkIdParams,
		checkDuplicatedEmail,
		checkDuplicatedDisplayName,
		checkDuplicatedEmailAndDisplayName,
		checkProfile
	} = require('../middleware/typeCheck')({
		logger,
		User,
		init
	})

	//@desc : 로컬 회원가입
	//@router: DELETE http://localhost:3001/api/users
	//@body : email: String, password: String, displayName: String, img: Object
	api.post('/', [checkRegisterUser, checkDuplicatedEmailAndDisplayName], async (req, res) => {
		let email = req.body.email
		let password = req.body.password
		let displayName = req.body.displayName
		let img = req.file

		return hasher({
			password: password
		}, async function (err, pass, salt, hash) {
			if (err) {
				logger.error(err.message)
				return res.status(500).json({
					message: err.message
				})
			}

			 //transform[0], [1]이 고정적이지 않음으로 고정적이게
			 let originalImg
			 let thumbnailImg
			 if (check.not.null(img.transforms)) { 
				img.transforms.forEach((o) => {
					if (o.id === "original") originalImg = o
					if (o.id === "thumbnail") thumbnailImg = o
				})
			}
		 
			let newUser = new User({
				type: 'local',
				email: email,
				password: hash,
				profile: originalImg.location ? originalImg.location : init.defaultProfile,
				thumbnail: thumbnailImg.location ? thumbnailImg.location : init.defaultProfile,
				salt: salt,
				displayName: displayName,
			})

			await newUser.save((err, newUser) => {
				if (err) {
					logger.error(err.message)
					return res.status(500).json({
						message: err.message
					})
				} else
					return res.status(201).json({
						data: newUser
					})
			})
		})
	})

	//@desc : 자신의 유저 정보 가져오기
	//@router : GET http://localhost:3001/api/users/me
	api.get('/me', checkLoggedIn, async (req, res) => {
		let userId = req.query.userId

		try {
			let followingQuery = { user: userId }
			let followingCount = await Following.count(followingQuery)

			let followerQuery = { followUser: userId }
			let followerCount = await Following.count(followerQuery)

			let memoCountQuery = { user: userId }
			let memoCount = await Memo.count(commentCountQuery)

			user.set('followingCount', followingCount)
    		user.set('followerCount', followerCount)
    		user.set('memoCount', memoCount)
    		user.set('following', false)

			return res.status(200).json({
				data: user
			})

		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}

		res.status(401).json({
			data: req.user
		})
	})

	//@desc : 특정 유저 정보 가져오기
	//@router : GET http://localhost:3001/api/users/:id
	//@params : id: String
	api.get('/:id', checkIdParams, async (req, res) => {
		let id = req.params.id

		try {
			let query = { '_id': id }
			let user = await User.findOne(query)
			
			let followingCountQuery = { user: id }
    		let followingCount = await Following.count(followingCountQuery)

    		let followerCountQuery = { followUser: id }
    		let followerCount = await Following.count(followerCountQuery)

    		let memoCountQuery = { user: id }
			let memoCount = await Memo.count(memoCountQuery)

			user.set('followingCount', followingCount)
    		user.set('followerCount', followerCount)
    		user.set('memoCount', memoCount)

			if (req.isAuthenticated()) {
				let userId = req.user._id
				let followingQuery = { user: userId, followUser: id }
				let count = await Following.count(followingQuery)

				user.set('following', count > 0)
			}

			return res.status(200).json({
				data: user
			})
		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 자신의 계정을 삭제함
	//@router : DELETE http://localhost:3001/api/users
	api.delete('/', checkLoggedIn, async (req, res) => {
		let myUserId = req.user._id

		try {
			await User.where('_id')
				.equals(myUserId)
				.deleteOne()

			return res.status(200).json({})

		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 자신의 이메일을 수정
	//@router : PATCH http://localhost:3001/api/users/email
	//@body : email: String
	api.patch('/email', [checkLoggedIn, checkRegisterUser, checkDuplicatedEmail], async (req, res) => {
		let myUserId = req.user._id
		let email = req.body.email

		let filter = {
			_id: myUserId
		}
		let update = {
			$set: {
				email: email,
			}
		}
		let option = {
			new: true,
			upsert: false
		}

		try {
			let user = await User.findOneAndUpdate(filter, update, option)
			return res.status(200).json({
				data: user
			})
		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	//@desc : 자신의 비밀번호를 수정
	//@router : POST http://localhost:3001/api/users/display_name
	//@body : displayName: String
	api.post('/password', passport.authenticate('local', {
		session: false,
	    failureRedirect: '/api/auth/session/fail',
	    failureFlash: true
	}), async (req, res) => {
		let myUserId = req.user._id
		let newPassword = req.body.newPassword

		return hasher({
			password: newPassword
		}, async function (err, pass, salt, hash) {
			if (err) {
				logger.error(err.message)
				return res.status(500).json({
					message: err.message
				})
			}

			let filter = {
				_id: myUserId
			}
			let update = {
				$set: {
					password: hash,
					salt: salt,
				}
			}
			let option = {
				new: true,
				upsert: false
			}

			try {
				let user = await User.findOneAndUpdate(filter, update, option)
				return res.status(200).json({
					data: user
				})
			} catch (err) {
				logger.error(err.message)
				return res.status(500).json({
					message: err.message
				})
			}
		})
	})

	//@desc : 자신의 DisplayName을 수정
	//@router : PATCH http://localhost:3001/api/users/display_name
	//@body : displayName: String
	api.patch('/display_name', [checkLoggedIn, checkRegisterUser, checkDuplicatedDisplayName], async (req, res) => {
		let myUserId = req.user._id
		let displayName = req.body.displayName

		let filter = {
				_id: myUserId
			}
			let update = {
				$set: {
					displayName: displayName,
				}
			}
			let option = {
				new: true,
				upsert: false
			}

			try {
				let user = await User.findOneAndUpdate(filter, update, option)
				return res.status(200).json({
					data: user
				})
			} catch (err) {
				logger.error(err.message)
				return res.status(500).json({
					message: err.message
				})
			}
	})

	//@desc : 자신의 프로필 사진을 수정
	//@router : PATCH http://localhost:3001/api/users/profile
	//@body : img: Object
	api.patch('/profile', [checkLoggedIn, checkProfile], async (req, res) => {
		let myUserId = req.user._id
		let img = req.file

		//transform[0], [1]이 고정적이지 않음으로 고정적이게
		let originalImg =''
		let thumbnailImg = ''

		if (check.not.null(img.transforms)) { 
			img.transforms.forEach((o) => {
				if (o.id === "original") originalImg = o
				if (o.id === "thumbnail") thumbnailImg = o
			})
		}
		
		let filter = {
			_id: myUserId
		}
		let update = {
			$set: {
				profile: originalImg.location ? originalImg.location : init.defaultProfile,
				thumbnail: thumbnailImg.location ? thumbnailImg.location : init.defaultProfile
			}
		}
		let option = {
			new: true,
			upsert: false 
		}

		try {
			let user = await User.findOneAndUpdate(filter, update, option)
			return res.status(200).json({
				data: user
			})
		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})


	//@desc : 자신의 프로필 사진 삭제
	//@router : DELETE http://localhost:3001/api/users/profile
	api.delete('/profile', checkLoggedIn, async (req, res) => {
		let myUserId = req.user._id

		let filter = {
			user: myUserId
		}
		let option = {
			projection: 'profile'
		}

		try {
			let user = await User.findOneAndDelete(filter, option)
			return res.status(200).json({
				data: user
			})
		} catch (err) {
			logger.error(err.message)
			return res.status(500).json({
				message: err.message
			})
		}
	})

	return api
}