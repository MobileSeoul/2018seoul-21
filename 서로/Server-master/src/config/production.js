module.exports = (app) => {
    const compression = require('compression')

    if (app.get('env') === 'production') {
        console.log('Applicatoin Name: Production');
        console.log(`NODE_ENV: ${process.env.NODE_ENV}`);
        console.log(`app: ${app.get('env')}`);

        app.use(compression())
    }
}
