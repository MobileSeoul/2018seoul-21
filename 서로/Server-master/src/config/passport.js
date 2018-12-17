module.exports = ({
  app,
  init
}) => {
  const passport = require('passport')
  const FacebookStrategy = require('passport-facebook').Strategy
  const LocalStrategy = require('passport-local').Strategy
  const User = require('../model/user')
  const bkfd2Password = require("pbkdf2-password")
  const hasher = bkfd2Password()
  const errorMessage = require('../error_message')
  
  app.use(passport.initialize())
  app.use(passport.session())

  passport.serializeUser(function (user, done) {
    done(null, user.id)
  })

  passport.deserializeUser(function (id, done) {
    User.findById(id, function (err, user) {
      done(err, user)
    })
  })

  passport.use(new LocalStrategy({
    usernameField: "email",
    passwordField: "password",
    passReqToCallback: true
  }, function (req, userEmail, password, done) {

    let filter = {
      email: userEmail
    }

    try {
      User.findOne(filter, function (err, user) {
        if (err) {
          return done(err)
        }
        if (!user) {
          return done(null, false, req.flash('error', 'Incorrect username.'))
        }

        return hasher({
          password: password,
          salt: user.salt
        }, function (err, pass, salt, hash) {
          if (hash === user.password) {
            return done(null, user);
          }
          return done(null, false, req.flash('error', 'Incorrect password.'));
        })
      })
    } catch (err) {
      logger.error(err.message)
      return res.status(500).json({
        message: err.message
      })
    }
  }))

  passport.use(new FacebookStrategy({
    clientID: init.clientID,
    clientSecret: init.clientSecret,
    callbackURL: "/api/auth/facebook/callback",
    profileFields: ['id', 'name', 'email', 'displayName', 'photos']
  }, async function (accessToken, refreshToken, profile, done) {
    let type = 'facebook'

    let filter = {
      email: email
    }
    let update = {
      $set: {
        type: type,
        email: profile.emails[0].value,
        displayName: profile.displayName,
        profile: profile.photos ? profile.photos[0].value : init.defaultProfile,
        thumbnail: profile.photos ? profile.photos[0].value : init.defaultProfile
      }
    }
    let option = {
      new: true,
      upsert: true 
    }

    try {
      let user = await User.findOneAndUpdate(filter, update, option)
      return done(null, user)

    } catch (err) {
      return done(err)
    }
  }))
}