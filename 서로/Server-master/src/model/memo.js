const mongoose = require('mongoose')
const Schema = mongoose.Schema

const geoSchema = new Schema({
  type: {
    type: String,
    default: 'Point'
  },
  coordinates: {
    type: [Number],
    default: []
  }
});

const memoSchema = new Schema({
  thumbnail: {
    type: String,
    defulat: ""
  },
  img: {
    type: String,
    default: ""
  },
  text: {
    type: String,
    default: ""
  },
  date: {
    type: Date,
    default: Date.now
  },
  tags: {
    type: [String],
    defulat: []
  },
  user: {
    type: Schema.Types.ObjectId,
    ref: 'User',
    required: true,

  },
  loc: geoSchema,
  address: {
    type: Object
  }
}, { strict: false });

memoSchema.index({
  loc: '2dsphere'
});

module.exports = mongoose.model('Memo', memoSchema)