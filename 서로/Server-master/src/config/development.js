module.exports = (app) => {
  const morgan = require('morgan');
  const debug = require('debug')('app:startup');

  if (app.get('env') === 'development') {
    console.log('Applicatoin Name: Development');
    console.log(`NODE_ENV: ${process.env.NODE_ENV}`);
    console.log(`app: ${app.get('env')}`);

    app.use(morgan('tiny'));
    debug('Morgan enabled...');
  }
}
