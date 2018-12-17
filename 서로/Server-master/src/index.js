const init = require('./config')
//@express
const express = require('express')
const app = express()
const router = express.Router()
const routes = require('./routes')({router, init})
const bodyParser = require('body-parser')
//@development
require('./config/development')(app)
//@production
require('./config/production')(app)
//@session
require('./config/session')({app, init})
//@passport
require('./config/passport')({app, init})
const flash = require('connect-flash')
const cookieParser = require('cookie-parser')
//@security
const helmet = require('helmet')
app.use(helmet())

app.use(cookieParser());
app.use(flash());
app.disable('x-powered-by')
app.use(helmet())
app.use(express.static('public'))
app.use(bodyParser.urlencoded({extended: false, limit: '100mb'}))
app.use(bodyParser.json())

app.use('/api', routes)

app.listen(init.port, () => {
console.log(`listening port: " ${init.port}...`)
})
