define [
    'marionette'
    'views/FileView'
], (Marionette, FileView) ->
    class FileTable extends Marionette.CompositeView
        tagName: 'table'
        className: 'table table-striped table-bordered'
        itemView: FileView
        template: '#tmpl-file-table'

        appendHtml: (collectionView, itemView) ->
            collectionView.$('tbody').append(itemView.el)
