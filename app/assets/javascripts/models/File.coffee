define [
    'use!backbone'
    ], (Backbone) ->
        class File extends Backbone.Model
            urlRoot: 'api/files'