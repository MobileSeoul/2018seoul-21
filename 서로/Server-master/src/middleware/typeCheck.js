module.exports = ({
    logger,
    init
}) => {
    const upload = require('../services/file-upload')({
        init
    })
    const single = upload.single('img')

    const errorMessage = require('../error_message')
    const check = require('check-types')
    const User = require('../model/user')
    const Comment = require('../model/comment')
    const EMAIL_REGEX = /^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])$/
    const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/
    const DISPLAY_NAME_REGEX = /^[^\s\t\n\r\`\~\!\@\#\$\%\^\&\*\(\)\+\=\[\]\\\{\}\|\;\'\:\"\,\.\/\<\>\?]+$/
    const HASHTAG_REGEX = /#.[^\s\d\t\n\r\.\*\\`~!@#$%^&()\-=+[{\]}|;:'",<>\/?]+/g //ex: #tag1#tag2 => #tag#tag HACK!! 1글자 안됨 ex) #a#b#c

    const checkEmojiAndMemo = function (req, res, next) {
        let emoji = req.body.emoji
        let memo = req.body.memo

        if (check.not.string(memo)) {
            return res.status(404).json({
                message: errorMessage.INVALID_POST_REQUEST + ' (memo)'
            })
        }

        if (check.not.string(emoji)) {
            return res.status(404).json({
                message: errorMessage.INVALID_POST_REQUEST + ' (emoji)'
            })
        }

        return next()
    }

    const checkEmoji = function (req, res, next) {
        let emoji = req.body.emoji

        if (check.not.string(emoji)) {
            return res.status(404).json({
                message: errorMessage.INVALID_POST_REQUEST + ' (emoji)'
            })
        }


        return next()
    }


    const checkDuplicatedMemoAndUserid = async function (req, res, next) {
        let myUserId = req.user._id
        let memo = req.body.memo

        try {
            //이미 작성한 코멘트가 있는지 확인
            const emojiQuery = {
                user: myUserId,
                memo: memo
            }
            let emojiCount = await Comment.count(emojiQuery)

            if (emojiCount > 0) {
                return res.status(409).json({
                    message: errorMessage.CONFLICT_PARAMETER + ' (user, memo)'
                })
            }
        } catch (err) {
            logger.error(err.message)
            res.status(500).json({
                message: err.message
            })
        }

        return next()
    }

    const checkRegisterUser = function (req, res, next) {
        single(req, res, function (err) {
            if (err) {
                return res.status(400).json({
                    message: errorMessage.UNEXPECTED_FIELD_ERROR + ' (img)'
                })
            }

            let img = req.file
            let email = req.body.email
            let password = req.body.password
            let displayName = req.body.displayName

            if (check.not.object(img)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (img)'
                })
            }

            if (check.not.string(email) || !EMAIL_REGEX.test(email)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (email)'
                })
            }

            if (check.not.string(password) || !PASSWORD_REGEX.test(password)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (password)'
                })
            }

            if (check.not.string(displayName) || !DISPLAY_NAME_REGEX.test(displayName)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (displayName)'
                })
            }

            return next()
        })
    }

    const checkIdParams = function (req, res, next) {
        let id = req.params.id

        if (check.not.string(id)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (id)'
            })
        }

        return next()
    }

    const checkUserIdQuery = function (req, res, next) {
        let userId = req.query.userId

        if (check.not.string(userId)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (userId)'
            })
        }

        return next()
    }

    const checkDuplicatedEmail = async function (req, res, next) {
        let email = req.body.email

        try {
            //이미 가입한 이메일 있는지 확인
            const emailQuery = {
                'email': email
            }
            let emailCount = await User.count(emailQuery)

            if (emailCount > 0) {
                return res.status(409).json({
                    message: errorMessage.CONFLICT_PARAMETER + ' (email)'
                })
            }

            return next()
        } catch (err) {
            logger.error(err.message)
            res.status(500).json({
                message: err.message
            })
        }
    }

    const checkDuplicatedDisplayName = async function (req, res, next) {
        let displayName = req.body.displayName

        try {
            //이미 가입한 displayName있는지 확인
            const displayNameQuery = {
                'displayName': displayName
            }
            let displayNameCount = await User.count(displayNameQuery)

            if (displayNameCount > 0) {
                return res.status(409).json({
                    message: errorMessage.CONFLICT_PARAMETER + ' (displayName)'
                })
            }

            return next()
        } catch (err) {
            logger.error(err.message)
            res.status(500).json({
                message: err.message
            })
        }
    }

    const checkDuplicatedEmailAndDisplayName = async function (req, res, next) {
        let email = req.body.email
        let displayName = req.body.displayName

        try {
            //이미 가입한 이메일 있는지 확인
            const emailQuery = {
                'email': email
            }
            let emailCount = await User.count(emailQuery)

            if (emailCount > 0) {
                return res.status(409).json({
                    message: errorMessage.CONFLICT_PARAMETER + ' (email)'
                })
            }

            //이미 가입한 displayName있는지 확인
            const displayNameQuery = {
                'displayName': displayName
            }
            let displayNameCount = await User.count(displayNameQuery)

            if (displayNameCount > 0) {
                return res.status(409).json({
                    message: errorMessage.CONFLICT_PARAMETER + ' (displayName)'
                })
            }

            return next()
        } catch (err) {
            logger.error(err.message)
            res.status(500).json({
                message: err.message
            })
        }
    }

    const checkProfile = function (req, res, next) {
        single(req, res, function (err) {
            if (err) {
                return res.status(400).json({
                    message: errorMessage.UNEXPECTED_FIELD_ERROR + ' (img)'
                })
            }

            let img = req.file

            if (check.not.object(img)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (img)'
                })
            }

            return next()
        })
    }

    const filterTags = function (req, res, text) {
        let hashtagsWithoutSharp = []
        let hashtags = text.match(HASHTAG_REGEX)

        if (!check.null(hashtags)) {
            hashtags.forEach(hashtag => {
                hashtagsWithoutSharp.push(hashtag.substring(1))
            })
        }

        return hashtagsWithoutSharp
    }

    const checkMemo = function (req, res, next) {
        single(req, res, function (err) {
            if (err) {
                return res.status(400).json({
                    message: errorMessage.UNEXPECTED_FIELD_ERROR + ' (img)'
                })
            }

            let text = req.body.text
            let img = req.file
            let loc = req.body.loc

            if (check.not.object(img)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (img)'
                })
            }
            
            if (check.not.object(loc)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (loc)'
                })
            }

            if (check.not.string(text)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_QUERY_PARAMETER + ' (text)'
                })
            }

            req.body.tags = filterTags(req, res, text)

            return next()
        })
    }

    const checkMemoTextAndImage = function (req, res, next) {
        single(req, res, function (err) {
            if (err) {
                return res.status(400).json({
                    message: errorMessage.UNEXPECTED_FIELD_ERROR + ' (img)'
                })
            }

            let img = req.file
            let text = req.body.text

            if (check.not.object(img)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_POST_REQUEST + ' (img)'
                })
            }

            if (check.not.string(text)) {
                return res.status(400).json({
                    message: errorMessage.INVALID_QUERY_PARAMETER + ' (text)'
                })
            }

            req.body.tags = filterTags(req, res, text)

            return next()
        })
    }

    const checkSkipAndLimit = function (req, res, next) {
        let skip = req.query.skip
        let limit = req.query.limit

        if (check.not.string(skip)) {
            skip = 0
        }

        if (check.not.string(limit)) {
            limit = 30
        }

        return next()
    }

    const checkLngAndLat = function (req, res, next) {
        let lng = req.query.lng
        let lat = req.query.lat

        if (check.not.string(lng)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (lng)'
            })
        }

        if (check.not.string(lat)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (lat)'
            })
        }

        return next()
    }

    const checkTag = function (req, res, next) {
        let tag = req.query.tag

        if (check.not.string(tag)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (tag)'
            })
        }

        return next()
    }

    const checkCountry = function (req, res, next) {
        let country = req.query.country

        if (check.not.string(country)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (country)'
            })
        }

        return next()
    }

    const checkState = function (req, res, next) {
        let state = req.query.state

        if (check.not.string(state)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (state)'
            })
        }

        return next()
    }

    const checkCity = function (req, res, next) {
        let city = req.query.city

        if (check.not.string(city)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (city)'
            })
        }

        return next()
    }

    const checkTown = function (req, res, next) {
        let town = req.query.town

        if (check.not.string(town)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (town)'
            })
        }

        return next()
    }

    const checkVillage = function (req, res, next) {
        let village = req.query.village

        if (check.not.string(village)) {
            return res.status(400).json({
                message: errorMessage.INVALID_QUERY_PARAMETER + ' (village)'
            })
        }

        return next()
    }

    return {
        checkEmojiAndMemo,
        checkEmoji,
        checkDuplicatedMemoAndUserid,
        checkRegisterUser,
        checkIdParams,
        checkUserIdQuery,
        checkDuplicatedEmail,
        checkDuplicatedDisplayName,
        checkDuplicatedEmailAndDisplayName,
        checkProfile,
        checkMemo,
        checkMemoTextAndImage,
        checkSkipAndLimit,
        checkLngAndLat,
        checkTag,
        checkCountry,
        checkState,
        checkCity,
        checkTown,
        checkVillage
    }
}