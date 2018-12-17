module.exports = (init) => {
  //@logger
  const winston = require('winston')
  require('winston-mongodb')
  const logger = winston.createLogger({
    level: 'debug',
    format: winston.format.json(),
    transports: [
      new winston.transports.MongoDB({
        level: 'error',
        db: init.mongoUrl
      })
    ]
  });

  //예측하지 못한 에러 뜰때 로그 파일에 저장
  process.on('uncaughtException', (err) => {
    logger.error(err.message, err)
  })

  //production일 때 콘솔에도 에러로그 띄움
  if (process.env.NODE_ENV !== 'production') {
    logger.add(new winston.transports.Console({
      format: winston.format.simple()
    }));
  }

  return logger;
}