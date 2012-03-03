define ['Backbone', 'jQuery', 'Handlebars', 'collections/FileList', 'views/FileView', 'text!templates/file/main.html', 'libs/jquery/jquery.iframe-transport'],
(Backbone, $, Handlebars, FileList, FileView, mainTemplate) ->
    
    class AppView extends Backbone.View
        el: $('#content')
            
        template: Handlebars.compile mainTemplate
        
        initialize: ->
            @Files = new FileList
            
            @Files.bind 'add', @addOne
            @Files.bind 'reset', @addAll
            
            @Files.fetch()
        
        fileinput: $('#fileinput')
        
        events:
            'submit form': 'newsend'
            'change #fileinput' : 'selectfile'
        
        render: =>
            @$el.html @template()
            @addAll()
            @
        
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
                console.log(data)
                @Files.add(data)
            )
            false
        
        addOne: (file) =>
            view = new FileView model: file
            $('#file-table tbody').append view.render().el
        
        addAll: =>
            @Files.each(@addOne)
            
    new AppView