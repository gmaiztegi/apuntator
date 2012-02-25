$ ->    
    class File extends Backbone.Model
        urlRoot: 'api/files'

    class FileList extends Backbone.Collection
        model: File
        url: 'api/files'

    window.Files = new FileList

    class FileView extends Backbone.View
        tagName: "tr"
        template: Handlebars.compile $('#file-template').html()
        initialize: ->
            @model.bind('change', @render)
            @model.bind('destroy', @render)
    
        render: =>
            @$el.html @template(@model.toJSON())
            this
        
        clear: ->
            @model.destroy()
    
    class AppView extends Backbone.View
        initialize: ->
            @setElement($('#form'))
            
            Files.bind 'add', @addOne
            Files.bind 'reset', @addAll
            Files.bind 'all', @render
            
            Files.fetch()
        
        fileinput: $('#fileinput')
        
        events:
            'submit form': 'newsend'
            'change #fileinput' : 'selectfile'
        
        render: =>
        
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
            }).done( (data) ->
                console.log(data)
                Files.add(data)
            )
            false
        
        addOne: (file) =>
            view = new FileView model: file
            $('#file-table tbody').append view.render().el
        
        addAll: =>
            Files.each(@addOne)
        
    window.App = new AppView