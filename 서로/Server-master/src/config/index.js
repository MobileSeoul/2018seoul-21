require('dotenv').config()

module.exports = {
  "port": process.env.PORT||3001,
  //"mongoUrl": "mongodb://localhost/seoul_app_contest",
  "mongoUrl" : process.env.mongoUrl,
  "SESSION_SECRET_KEY": process.env.SESSION_SECRET_KEY,
  "AWS_ACCESS_KEY": process.env.AWS_ACCESS_KEY,
  "AWS_SECRET_ACCESS_KEY": process.env.AWS_SECRET_ACCESS_KEY,
  "HerokuUrl": "https://whispering-sea-68497.herokuapp.com",
  "clientID": process.env.clientID,
  "clientSecret": process.env.clientSecret,
  "email": process.env.email,
  "defaultProfile": "https://s3.ap-northeast-2.amazonaws.com/seoul-image-server/default_profile.png"
} 
