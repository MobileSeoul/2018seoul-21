const mongoose = require('mongoose')
const Schema = mongoose.Schema

const commentSchema = new Schema({
  memo: {
    type: Schema.Types.ObjectId,
    ref: 'Memo'
  },
  user: {
    type: Schema.Types.ObjectId,
    ref: 'User'
  },
  emoji: {
    type: String,
    enum: ['HAPPY', 'SAD', 'LIKE', 'DISLIKE']
  },
  date: {
    type: Date,
    default: Date.now
  }
})

module.exports = mongoose.model('Comment', commentSchema)
