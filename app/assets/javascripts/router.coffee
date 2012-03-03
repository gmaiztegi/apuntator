define ['jQuery', 'Underscore', 'Backbone', 'views/AppView'],
($, _, Backbone, AppView) ->
    class AppRouter extends Backbone.Router
        routes:
            '': 'indexAction'
            '*default': 'defaultAction'
        
        indexAction: ->
            AppView.render()
        
        defaultAction: ->
            console.log 'No route!'
        
    
    initialize = ->
        app_router = new AppRouter
        Backbone.history.start({pushState: true})
    
    {
        initialize: initialize
    }