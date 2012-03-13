define [
    'jquery'
    'underscore'
    'backbone'
    'router'
    'polyfiller'
], ($, _, Backbone, Router) ->
    initialize = ->
        Router.initialize()
  
    $.webshims.setOptions {
        waitReady: false,
        basePath: '/assets/javascripts/libs/webshims/shims/'
    }
  
    $.webshims.polyfill()
  
    {
        initialize: initialize
    }