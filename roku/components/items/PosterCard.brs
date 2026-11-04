sub init()
    m.art = m.top.findNode("art")
    m.title = m.top.findNode("title")
    m.initial = m.top.findNode("initial")
    m.ring = m.top.findNode("ring")
    m.favBox = m.top.findNode("favBox")
    m.fav = m.top.findNode("fav")
end sub

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    t = c.title
    isFav = (Left(t, 2) = "* ")
    if isFav then t = Mid(t, 3)
    m.title.text = t
    m.initial.text = UCase(Left(t, 1))
    m.favBox.visible = isFav
    m.fav.visible = isFav
    ' Los canales (en Buscar) muestran su logo completo, sin recortar.
    if c.ShortDescriptionLine2 = "live"
        m.art.loadDisplayMode = "scaleToFit"
        m.art.translation = [14, 50]
        m.art.width = 100
        m.art.height = 92
    else
        m.art.loadDisplayMode = "scaleToZoom"
        m.art.translation = [0, 0]
        m.art.width = 128
        m.art.height = 192
    end if
    m.art.uri = c.HDPosterUrl
    paint()
end sub

sub paint()
    on = m.top.gridHasFocus and m.top.focusPercent > 0.5
    m.ring.visible = on
    if on then m.title.color = "0xFFFFFFFF" else m.title.color = "0xCDB8C9FF"
end sub
