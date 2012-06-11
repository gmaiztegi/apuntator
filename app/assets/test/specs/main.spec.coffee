require.config {
    baseUrl: '/assets/javascripts'
    paths:
        domReady: 'libs/require/domReady'
        order: 'libs/require/order'
        text: 'libs/require/text'
        use: 'libs/require/use'
        jquery: 'libs/jquery/jquery'
        underscore: 'libs/underscore/underscore'
        backbone: 'libs/backbone/backbone-0.9.2'
        marionette: 'libs/backbone/backbone.marionette'
        handlebars: 'libs/handlebars/handlebars'
        polyfiller: 'libs/webshims/polyfiller'
        'iframe-transport': 'libs/jquery/jquery.iframe-transport'
        specs: '../../@test/assets/specs'

    use:
        handlebars:
            attach: ->
                Handlebars
}

jasmineEnv = jasmine.getEnv()
jasmineEnv.updateInterval = 1000

htmlReporter = new jasmine.HtmlReporter()

jasmineEnv.addReporter htmlReporter

jasmineEnv.specFilter = (spec) ->
    htmlReporter.specFilter spec

require ['specs/models.spec'], ->
    jasmineEnv.execute()
