const mongoose = require('mongoose')
const Schema = mongoose.Schema

const followingSchema = new Schema({
	user: {
		type: Schema.Types.ObjectId,
		ref: 'User'
	},
	followUser: {
		type: Schema.Types.ObjectId,
		ref: 'User'
	},
	date: {
		type: Date,
	    default: Date.now
	}
})

module.exports = mongoose.model('Following', followingSchema)