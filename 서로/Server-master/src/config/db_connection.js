module.exports = (callback) => {
  const mongoose = require('mongoose')
  const init = require('./')

  mongoose.connect(init.mongoUrl)
  .then(() => console.log('Connected to MongoDB...'))
  .catch(err => console.error(('Could not connect to MongoDB...\n'), err))

  const conn = mongoose.connection;
  conn.on('open', () => {
    return callback(conn.db);
  });
}
