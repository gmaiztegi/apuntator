define [
    'jquery'
    'backbone'
    'ApuntatorApp'
    'views/MenuView'
    'polyfiller'
    'marionette'
    'use!template'
], ($, Backbone, App, MenuView) ->
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
    
    Backbone.Marionette.Renderer.renderTemplate = (template, data) ->
        $.tmpl template, data
    
    $.webshims.polyfill()
  
    {
        initialize: initialize
    }