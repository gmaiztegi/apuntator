require.config {
    paths:
        domReady: 'libs/require/domReady'
        order: 'libs/require/order'
        text: 'libs/require/text'
        use: 'libs/require/use'
        jquery: 'libs/jquery/jquery'
        template: 'libs/jquery/jquery.tmpl'
        underscore: 'libs/underscore/underscore'
        backbone: 'libs/backbone/backbone-0.9.2'
        marionette: 'libs/backbone/backbone.marionette'
        handlebars: 'libs/handlebars/handlebars'
        polyfiller: 'libs/webshims/polyfiller'
        'iframe-transport': 'libs/jquery/jquery.iframe-transport'

    use:
        template:
            deps: ['jquery']
}

require ['app'], (App) ->
    App.initialize()
