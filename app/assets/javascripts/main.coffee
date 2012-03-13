require.config {
    paths:
        jquery: 'libs/jquery/jquery-1.7.1'
        underscore: 'libs/underscore/underscore'
        backbone: 'libs/backbone/backbone'
        handlebars: 'libs/handlebars/handlebars'
        polyfiller: 'libs/webshims/polyfiller'
        'iframe-transport': 'libs/jquery/jquery.iframe-transport'
}

require ['app'], (App) ->
    App.initialize()
