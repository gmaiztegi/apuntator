require.config({
    paths: {
        jQuery: 'libs/jquery/jquery',
        jquery: 'libs/jquery/jquery',
        Underscore: 'libs/underscore/underscore',
        Backbone: 'libs/backbone/backbone',
        Handlebars: 'libs/handlebars/handlebars'
    }
});

require([
    'app',
    'order!libs/jquery/jquery-1.7.1',
    'order!libs/underscore/underscore-0',
    'order!libs/backbone/backbone-0.9.1'
    ], function(App){
    App.initialize();
});