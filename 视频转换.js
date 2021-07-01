
    // //转MP4、合成音频
    // async getMp4(blob) {
    //   console.log('blob,', blob)
    //   let startTime = new Date()
    //   let file = new window.File(blob, 'webmVideo', {
    //     type: 'video/webm'
    //   })
    //   console.log('file,', file)

    //   const { createFFmpeg, fetchFile } = FFmpeg
    //   const ffmpeg = createFFmpeg({
    //     log: true,
    //     // eslint-disable-next-line no-unused-vars
    //     progress: ({ ratio }) => {
    //       // console.log(`Complete: ${(ratio * 100.0).toFixed(2)}%`)
    //     }
    //   })

    //   const { name } = file
    //   await ffmpeg.load()
    //   ffmpeg.FS('writeFile', name, await fetchFile(file))
    //   let musicBlob = await this.getMusic()

    //   //转码  webm => mp4
    //   await ffmpeg.run('-i', name, 'outputMp4Video.mp4')
    //   console.log('BBT 转码完成')
    //   //构建音频
    //   // let musicBlob = await this.getMusic()
    //   let fileMusic = new window.File(musicBlob, 'mp3Music', {
    //     type: 'audio/mpeg'
    //   })
    //   const nameMusic = 'mp3Music'
    //   ffmpeg.FS('writeFile', nameMusic, await fetchFile(fileMusic))
    //   console.log('BBT 加载音乐完毕')
    //   //剪切音乐
    //   let musicTime = Math.ceil(this.images.length * 2.25)
    //   musicTime = musicTime < 10 ? `0${musicTime}` : musicTime
    //   // # ffmpeg -i aa.mp3 -ss 00:01:12 -t 00:01:42 -acodec copy bb.mp3
    //   await ffmpeg.run(
    //     '-i',
    //     nameMusic,
    //     '-ss',
    //     '00:00:00',
    //     '-t',
    //     `00:00:${musicTime}`,
    //     '-acodec',
    //     'copy',
    //     'mp3MusicNew.mp3'
    //   )
    //   //加背景音乐
    //   // ffmpeg -i 1.mp3 -i 1.mp4 outputname.mp4
    //   await ffmpeg.run(
    //     '-i',
    //     'mp3MusicNew.mp3',
    //     '-i',
    //     'outputMp4Video.mp4',
    //     'endMusicMp4Video.mp4'
    //   )
    //   console.log('BBT 剪切背景音乐 合成音乐 结束')

    //   //最终数据
    //   const data = ffmpeg.FS('readFile', 'endMusicMp4Video.mp4')
    //   console.log('BBT 加载最终视频文件')
    //   // let byte = new Blob([data.buffer], {
    //   //   type: 'video/mp4'
    //   // })
    //   // console.log('byte,', byte)
    //   let aTemp = document.createElement('a')
    //   aTemp.href = URL.createObjectURL(
    //     new Blob([data.buffer], {
    //       type: 'video/mp4'
    //     })
    //   )
    //   aTemp.download = '40Test.mp4'
    //   aTemp.click()
    //   const video = document.getElementById('video-mp4')
    //   video.src = URL.createObjectURL(
    //     new Blob([data.buffer], {
    //       type: 'video/mp4'
    //     })
    //   )
    //   let endTime = new Date()
    //   let mm = endTime.getTime() - startTime.getTime()
    //   console.log('视频时长：', this.videoDuration, ';转换时间：', mm / 1000)
    // },
    // //加载音乐资源
    // getMusic() {
    //   let that = this
    //   return new Promise(function(res, rej) {
    //     let musicBlob = []
    //     let xhr = new XMLHttpRequest()
    //     xhr.open('get', that.musicUrl, true)
    //     xhr.responseType = 'blob'
    //     xhr.onload = function() {
    //       if (xhr.status == 200) {
    //         musicBlob.push(xhr.response)
    //         console.log(musicBlob)
    //         res(musicBlob)
    //       } else {
    //         rej()
    //       }
    //     }
    //     xhr.send()
    //   })
    // },