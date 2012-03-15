define ['backbone', 'jquery'],
(Backbone, $) ->
        class FileView extends Backbone.Marionette.ItemView
            tagName: 'tr'
            template: '#tmpl-file-row'
