define [
    'jquery'
    'backbone'
    'use!handlebars'
    'ApuntatorApp'
    'views/MenuView'
    'polyfiller'
    'marionette'
], ($, Backbone, Handlebars, App, MenuView) ->
    initialize = ->
        App.start()

    App.addInitializer ->
        App.menu.show MenuView

    #App.addInitializer ->
        #new Router
        #Backbone.history.start()
    
    $.webshims.setOptions
        waitReady: false,
        basePath: '/assets/javascripts/libs/webshims/shims/'

    Backbone.Marionette.TemplateCache.compileTemplate = (rawTemplate) ->
        Handlebars.compile(rawTemplate)
    
    $.webshims.polyfill()
  
    {
        initialize: initialize
    }