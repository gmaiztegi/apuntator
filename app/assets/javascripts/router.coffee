define ['use!marionette', 'ApuntatorApp', 'views/FilesView', 'views/RegisterView'],
(Marionette, App, FilesView, RegisterView) ->
    class Router extends Marionette.AppRouter
        appRoutes:
            '': 'files'
            'files': 'files'
            'register': 'register'
            '*default': 'default'
        
        controller:
            files: ->
                App.main.show(FilesView)

            register: ->
                App.main.show(RegisterView)

            default: ->
