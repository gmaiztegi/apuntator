define [
    'jquery'
    'use!backbone'
    'router'
    'ApuntatorApp'
    'views/MenuView'
    'polyfiller'
    'use!marionette'
    'use!template'
], ($, Backbone, Router, App, MenuView) ->
    initialize = ->
        App.start()

    App.addInitializer ->
        App.menu.show MenuView

    App.addInitializer ->
        new Router
        Backbone.history.start()
    
    $.webshims.setOptions {
        waitReady: false,
        basePath: '/assets/javascripts/libs/webshims/shims/'
    }
    
    Backbone.Marionette.ItemView.prototype.renderTemplate = (template, data) ->
        $.tmpl template, data
    
    $.webshims.polyfill()
  
    {
        initialize: initialize
    }