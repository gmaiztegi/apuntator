define ['use!marionette', 'jquery'],
(Marionette, $) ->
        class FileView extends Marionette.ItemView
            tagName: 'tr'
            template: '#tmpl-file-row'
