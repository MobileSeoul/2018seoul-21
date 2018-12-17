const errorMessage = require('../error_message')

//로그인이 되어있는지 확인, 로그인 안되어있을시 실패 메세지 반환
const checkLoggedIn = function(req, res, next) {
  if (req.isAuthenticated()) {
    return next()
  }
  res.status(401).json({ message: errorMessage.ACCESS_DENIED })
}

//로그인이 안되있는지 확인, 로그인 되어있을시 실패 메세지 반환
const checkLoggedOut = function(req, res, next) {
  if (!req.isAuthenticated()) {
    return next()
  }
  res.status(404).json({ message: errorMessage.SESSION_NOT_FOUND })
}

module.exports = {
  checkLoggedIn,
  checkLoggedOut
}
