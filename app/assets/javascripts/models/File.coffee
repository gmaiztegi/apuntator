define [
    'Backbone'
    ], (Backbone) ->
        class File extends Backbone.Model
            urlRoot: 'api/files'