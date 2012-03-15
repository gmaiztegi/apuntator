define [
    'jquery'
    'underscore'
    'backbone'
    'router'
    'ApuntatorApp'
    'polyfiller'
], ($, _, Backbone, Router, App) ->
    initialize = ->
        App.start()
    
    App.addInitializer ->
        new Router
        Backbone.history.start()
    
    $.webshims.setOptions {
        waitReady: false,
        basePath: '/assets/javascripts/libs/webshims/shims/'
    }
    
    Backbone.Marionette.ItemView.prototype.renderTemplate = (template, data) ->
        template.tmpl data
    
    $.webshims.polyfill()
  
    {
        initialize: initialize
    }