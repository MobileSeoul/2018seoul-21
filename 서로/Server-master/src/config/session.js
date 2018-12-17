module.exports = ({app, init}) => {
  const Store = require('express-session').Store;
  const MongooseStore = require('mongoose-express-session')(Store);
  const mongoose = require('mongoose');

  app.use(require('express-session')({
      secret: init.SESSION_SECRET_KEY,
      resave: false,
      rolling: false,
      saveUninitialized: true,
      maxAge: Date.now() + (30 * 86400 * 1000), //30 days
      store: new MongooseStore({ connection: init.mongoUrl, mongoose: mongoose }),
      cookie:{ maxAge:Date.now() + (30 * 86400 * 1000),  httpOnly: true } //30days 
  }));
}
