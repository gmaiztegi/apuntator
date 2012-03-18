define [
    'use!marionette'
    'jquery'
    'collections/FileList'
    'views/FileTable'
    'views/MenuView'
    'models/MenuItem'
    'iframe-transport'
], (Marionette, $,  FileList, FileTable, MenuView, MenuItem) ->
    
    class FilesView extends Marionette.Layout
            
        template: '#tmpl-file-main'
        
        regions:
            table: '#file-table'
        
        events:
            'submit form': 'newsend'
            'change #fileinput' : 'selectfile'
        
        menuitem: new MenuItem
            title: 'Ficheros'
            tag: 'files'
            order: 50

        initialize: ->
            @files = new FileList
            @tableview = new FileTable
                collection: @files

            MenuView.leftitems.add @menuitem

        selectfile: (event) =>
            @$('.file').hide()
            @$('.data').show()
            filelist = event.target.files
            file = filelist.item(0)
            @$('#filename').text file.name
        
        newsend: (event) =>
            $.ajax(event.target.action, {
                data: $(event.target).serializeArray()
                files: @$(':file')
                processData: false
                type: 'POST'
                iframe: true
                dataType: 'json'
            }).done( (data) =>
                @files.add(data)
            )
            false
        
        onRender: ->
            @fileinput = @$('#fileinput')
            
            @files.fetch()
            
            @table.show @tableview, 'append'
            
    new FilesView
