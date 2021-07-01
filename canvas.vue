<template>
  <div class="player-wrap">
    <canvas id="player" :width="canvasWidth" height="400">
      Your browser does not support the HTML5 canvas tag.
    </canvas>
    <video
      style="border: 1px solid black;margin-left:100px;"
      :width="canvasWidth"
      height="400"
      controls="true"
      autoplay="true"
      id="video-mp4"
    ></video>
  </div>
</template>

<script>
import FFmpeg from '@ffmpeg/ffmpeg'
export default {
  name: 'videoCanvas',
  props: {
    mode: {
      type: String,
      default: '-1'
    },
    musicUrl: {
      type: String,
      default: ''
    },
    videoStyle: {
      type: Number,
      default: 1
    },
    images: {
      type: Array,
      default() {
        return []
      }
    }
  },
  watch: {},

  data() {
    return {
      ctx: {},
      width: 0,
      height: 0,
      imgList: [],
      imgLoaded: 0,
      specialType: 1
    }
  },
  computed: {
    canvasWidth() {
      return this.mode == '1' ? 400 : this.mode == '2' ? 300 : 400
    },
    videoDuration() {
      return this.images.length * (2.25 + 0.03)
    }
  },
  created() {},
  mounted() {
    this.loadImg()
  },
  destroyed() {
    clearTimeout(this.oneTimeOut)
    clearTimeout(this.twoTimeOut)
  },
  methods: {
    loadImg() {
      this.imgList = []
      for (var i = this.images.length; i--; ) {
        var img = new Image()
        img.crossOrigin = 'Anonymous'
        img.addEventListener('load', () => {
          onload()
        })
        img.src = this.images[i]
        this.imgList.push(img)
        let onload = () => {
          this.imgLoaded++
          if (this.imgLoaded == this.images.length) {
            this.imgList.reverse()
            this.specialCanvas() //全部加载完成调用动画
          }
        }
      }
    },
    getVideo() {
      console.log('获取视频')
      const stream = document.querySelector('canvas').captureStream()
      const recorder = new MediaRecorder(stream, {
        mimeType: 'video/webm',
        videoBitsPerSecond: 100 * 1024 * 1024
      })

      const data = []
      recorder.ondataavailable = function(event) {
        if (event.data && event.data.size) {
          data.push(event.data)
        }
      }
      recorder.onstop = () => {
        console.log('结束录制')
        let aTemp = document.createElement('a')
        aTemp.href = URL.createObjectURL(
          new Blob(data, {
            type: 'video/mp4'
          })
        )
        aTemp.download = '40TestMor.mp4'
        aTemp.click()

        let video = document.querySelector('#video-mp4')
        video.src = URL.createObjectURL(
          new Blob(data, {
            type: 'video/mp4'
          })
        )
        // this.getMp4(data)
      }
      recorder.start()
      setTimeout(() => {
        recorder.stop()
      }, (this.videoDuration + 2) * 1000)
    },
    //转MP4、合成音频
    async getMp4(blob) {
      console.log('blob,', blob)
      let startTime = new Date()
      let file = new window.File(blob, 'webmVideo', {
        type: 'video/webm'
      })
      console.log('file,', file)

      const { createFFmpeg, fetchFile } = FFmpeg
      const ffmpeg = createFFmpeg({
        log: true,
        // eslint-disable-next-line no-unused-vars
        progress: ({ ratio }) => {
          // console.log(`Complete: ${(ratio * 100.0).toFixed(2)}%`)
        }
      })

      const { name } = file
      await ffmpeg.load()
      ffmpeg.FS('writeFile', name, await fetchFile(file))
      let musicBlob = await this.getMusic()

      //转码  webm => mp4
      await ffmpeg.run('-i', name, 'outputMp4Video.mp4')
      console.log('BBT 转码完成')
      //构建音频
      // let musicBlob = await this.getMusic()
      let fileMusic = new window.File(musicBlob, 'mp3Music', {
        type: 'audio/mpeg'
      })
      const nameMusic = 'mp3Music'
      ffmpeg.FS('writeFile', nameMusic, await fetchFile(fileMusic))
      console.log('BBT 加载音乐完毕')
      //剪切音乐
      let musicTime = Math.ceil(this.images.length * 2.25)
      musicTime = musicTime < 10 ? `0${musicTime}` : musicTime
      // # ffmpeg -i aa.mp3 -ss 00:01:12 -t 00:01:42 -acodec copy bb.mp3
      await ffmpeg.run(
        '-i',
        nameMusic,
        '-ss',
        '00:00:00',
        '-t',
        `00:00:${musicTime}`,
        '-acodec',
        'copy',
        'mp3MusicNew.mp3'
      )
      //加背景音乐
      // ffmpeg -i 1.mp3 -i 1.mp4 outputname.mp4
      await ffmpeg.run(
        '-i',
        'mp3MusicNew.mp3',
        '-i',
        'outputMp4Video.mp4',
        'endMusicMp4Video.mp4'
      )
      console.log('BBT 剪切背景音乐 合成音乐 结束')

      //最终数据
      const data = ffmpeg.FS('readFile', 'endMusicMp4Video.mp4')
      console.log('BBT 加载最终视频文件')
      // let byte = new Blob([data.buffer], {
      //   type: 'video/mp4'
      // })
      // console.log('byte,', byte)
      let aTemp = document.createElement('a')
      aTemp.href = URL.createObjectURL(
        new Blob([data.buffer], {
          type: 'video/mp4'
        })
      )
      aTemp.download = '40Test.mp4'
      aTemp.click()
      const video = document.getElementById('video-mp4')
      video.src = URL.createObjectURL(
        new Blob([data.buffer], {
          type: 'video/mp4'
        })
      )
      let endTime = new Date()
      let mm = endTime.getTime() - startTime.getTime()
      console.log('视频时长：', this.videoDuration, ';转换时间：', mm / 1000)
    },
    //加载音乐资源
    getMusic() {
      let that = this
      return new Promise(function(res, rej) {
        let musicBlob = []
        let xhr = new XMLHttpRequest()
        xhr.open('get', that.musicUrl, true)
        xhr.responseType = 'blob'
        xhr.onload = function() {
          if (xhr.status == 200) {
            musicBlob.push(xhr.response)
            console.log(musicBlob)
            res(musicBlob)
          } else {
            rej()
          }
        }
        xhr.send()
      })
    },
    async specialCanvas() {
      let canvas = document.getElementById('player')
      this.ctx = canvas.getContext('2d')
      this.width = canvas.width
      this.height = canvas.height
      this.ctx.clearRect(0, 0, this.width, this.height)
      this.specialType = 1
      let maxLenght = this.imgList.length
      let startTime = new Date()
      this.getVideo()
      for (let index1 = 0; index1 < maxLenght; index1++) {
        let indexTemp = index1
        let indexTemp2 = index1 + 1
        if (index1 == maxLenght - 1) {
          indexTemp2 = 0
        }
        if (this.videoStyle == 1) {
          await this.upEnlarge(indexTemp, indexTemp2) //上下放大
        } else if (this.videoStyle == 2) {
          await this.fadeIn(indexTemp, indexTemp2) //淡入淡出
        } else if (this.videoStyle == 3) {
          await this.shape(indexTemp, indexTemp2) //多形状过渡
        } else if (this.videoStyle == 4) {
          await this.pixelsDot(indexTemp, indexTemp2) //像素溶解过渡
        } else if (this.videoStyle == 5) {
          await this.leafWindow(indexTemp, indexTemp2) //百叶窗
        } else if (this.videoStyle == 6) {
          await this.rotate(indexTemp, indexTemp2) //旋转过渡
        } else if (this.videoStyle == 7) {
          await this.bevelAngle(indexTemp, indexTemp2) //斜角过渡
        } else if (this.videoStyle == 8) {
          await this.pushOut(indexTemp, indexTemp2) //挤压过渡
        } else if (this.videoStyle == 9) {
          await this.rightAngle(indexTemp, indexTemp2) //直角过渡
        } else if (this.videoStyle == 10) {
          await this.flyIn(indexTemp, indexTemp2) //两侧飞入
        }
      }
      // let imgTemp = this.imgList[0]
      // this.ctx.drawImage(
      //   imgTemp,
      //   0,
      //   0,
      //   imgTemp.width,
      //   imgTemp.height,
      //   0,
      //   0,
      //   this.width,
      //   this.height
      // )
      let endTime = new Date()
      let mm = endTime.getTime() - startTime.getTime()
      console.log(
        '特效：',
        this.videoStyle,
        '当前图片数量：',
        this.imgList.length,
        '动画运行时间：',
        mm / 1000,
        '理论规定时间：',
        2.25 * this.imgList.length
      )
    },
    //计算左右位移 0 -> 10 -> 0 -> -10 -> 0
    getStep(move1) {
      let step
      if (move1 < 10) {
        step = move1
      } else if (move1 < 20) {
        step = 20 - move1
      } else if (move1 < 30) {
        step = 20 - move1
      } else if (move1 < 40) {
        step = -40 + move1
      }
      return step
    },
    //上下放大
    upEnlarge(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType
          let enlarge = 1.0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (enlarge >= 1.1) {
              //第二部分动作
              let h0 = 0
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              ;(function two() {
                if (h0 > that.height) {
                  res()
                  return
                }
                let y = 0
                if (that.specialType == 1) {
                  //从上往下
                  y = -that.height + h0
                  h0 += 25
                } else {
                  //从下往上
                  y = that.height - h0
                  h0 += 25
                }
                that.ctx.drawImage(
                  img2,
                  0,
                  0,
                  w2,
                  h2,
                  0,
                  y,
                  that.width,
                  that.height
                )
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              that.ctx.save()
              that.ctx.scale(enlarge, enlarge)
              enlarge += 0.002638
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                (-that.width * (enlarge - 1)) / 2,
                (-that.height * (enlarge - 1)) / 2,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //淡入淡出
    fadeIn(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          let alpha1 = 100,
            alpha2 = 0
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              ;(function two() {
                if (alpha1 <= 0) {
                  res()
                  return
                }

                // 透明度
                that.ctx.globalAlpha = alpha1 / 100
                alpha1 -= 7
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
                that.ctx.globalAlpha = alpha2 / 100
                alpha2 += 7
                that.ctx.drawImage(
                  img2,
                  0,
                  0,
                  w2,
                  h2,
                  0,
                  0,
                  that.width,
                  that.height
                )
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作：左右移动
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 1
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //多形状过渡
    shape(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              let r = 0 //半径
              let x = 200,
                y = 200,
                w = 0,
                h = 0
              ;(function two() {
                if (that.specialType == -1) {
                  if (r >= 200 * Math.sqrt(2)) {
                    res()
                    return
                  }
                  r += 20
                  that.ctx.save()
                  that.ctx.beginPath()
                  that.ctx.arc(200, 200, r, 0, Math.PI * 2, true)
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                } else {
                  if (x <= 0) {
                    res()
                    return
                  }
                  x -= 15
                  y -= 15
                  w += 30
                  h += 30
                  that.ctx.save()
                  that.ctx.beginPath() //新建一条路径
                  that.ctx.rect(x, y, w, h)
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                }
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              // that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 0.9999
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //像素溶解过渡
    pixelsDot(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          // eslint-disable-next-line no-unused-vars
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              // let numX = 20
              // let numY = 20
              let num = 20
              let maxH = that.height / num
              let maxW = that.width / num
              let ratio = w2 / that.width
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              let indexNow = 0

              let imgArr = []
              let step = 40
              for (let i = 0; i < num; i++) {
                for (let j = 0; j < num; j++) {
                  let obj = {}
                  obj.sx = ratio * maxW * i
                  obj.sy = ratio * maxH * j
                  obj.swidth = ratio * maxW
                  obj.sheight = ratio * maxH

                  obj.x = maxW * i
                  obj.y = maxH * j
                  obj.width = maxW
                  obj.height = maxH
                  imgArr.push(obj)
                }
              }
              imgArr.sort(function() {
                return 0.5 - Math.random()
              })
              ;(function two() {
                if (indexNow >= imgArr.length) {
                  res()
                  return
                }
                for (let i = 0; i < step; i++) {
                  let obj = imgArr[i + indexNow]
                  that.ctx.drawImage(
                    img2,
                    obj.sx,
                    obj.sy,
                    obj.swidth,
                    obj.sheight,
                    obj.x,
                    obj.y,
                    obj.width,
                    obj.height
                  )
                }
                indexNow += step
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              that.ctx.save()
              let step = that.getStep(move1)
              move1 += 0.9
              if (that.specialType == -1) {
                that.ctx.translate(step, 0)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
              } else {
                that.ctx.translate(0, step)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
              }
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //百叶窗
    leafWindow(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              let num = 20
              let objH = 0
              let objW = 0
              let maxH = that.height / num
              let maxW = that.width / num
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              ;(function two() {
                if (that.specialType == -1) {
                  if (objH >= maxH) {
                    res()
                    return
                  }
                  objH = objH + 1.25
                  for (let i = 0; i < num; i++) {
                    let ratio = w2 / that.width
                    that.ctx.drawImage(
                      img2,
                      0,
                      ratio * maxH * i,
                      w2,
                      ratio * objH,
                      0,
                      maxH * i,
                      that.width,
                      objH
                    )
                  }
                } else {
                  if (objW >= maxW) {
                    res()
                    return
                  }
                  objW = objW + 1.25
                  for (let i = 0; i < num; i++) {
                    let ratio = w2 / that.width
                    that.ctx.drawImage(
                      img2,
                      ratio * maxW * i,
                      0,
                      ratio * objW,
                      h2,
                      maxW * i,
                      0,
                      objW,
                      that.height
                    )
                  }
                }
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              // that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.save()
              let step = that.getStep(move1)
              move1 += 1.04
              if (that.specialType == -1) {
                that.ctx.translate(step, 0)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
              } else {
                that.ctx.translate(0, step)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
              }
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //旋转过渡
    rotate(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType
          let enlarge = 1.0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (enlarge >= 1.1) {
              //第二部分动作
              let rotate = 0
              let dot = {
                x: 100,
                y: 100
              }
              let elg = 0.5
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              ;(function two() {
                if (rotate >= 360) {
                  res()
                  return
                }
                rotate += 24
                dot.x -= 7
                dot.y -= 7
                elg += 0.035
                that.ctx.clearRect(0, 0, that.width, that.height)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
                that.ctx.save()
                let dotTemp = {
                  x: dot.x + (that.width * elg) / 2,
                  y: dot.y + (that.height * elg) / 2
                }
                that.ctx.translate(dotTemp.x, dotTemp.y)
                that.ctx.rotate((rotate * Math.PI) / 180)
                that.ctx.translate(-dotTemp.x, -dotTemp.y)
                that.ctx.drawImage(
                  img2,
                  0,
                  0,
                  w2,
                  h2,
                  dot.x,
                  dot.y,
                  that.width * elg,
                  that.height * elg
                )
                that.ctx.restore()
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              that.ctx.save()
              that.ctx.scale(enlarge, enlarge)
              enlarge += 0.00245
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                (-that.width * (enlarge - 1)) / 2,
                (-that.height * (enlarge - 1)) / 2,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //斜角过渡
    bevelAngle(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType
          let step = 50
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              let dot1 = {
                x: 0,
                y: 0
              }
              let dot2 = {
                x: 0,
                y: 0
              }
              let dot3 = {
                x: 400,
                y: 400
              }
              let dot4 = {
                x: 400,
                y: 400
              }
              ;(function two() {
                if (that.specialType == -1) {
                  if (dot1.y >= 400) {
                    res()
                    return
                  }
                  if (dot1.x < 400) {
                    dot1.x += step
                    dot2.y += step
                  } else {
                    dot1.y += step
                    dot2.x += step
                  }
                  that.ctx.save()
                  that.ctx.beginPath()
                  that.ctx.moveTo(0, 0)
                  if (dot1.x < 400) {
                    that.ctx.lineTo(dot1.x, 0)
                    that.ctx.lineTo(0, dot2.y)
                  } else {
                    that.ctx.lineTo(dot1.x, 0)
                    that.ctx.lineTo(dot1.x, dot1.y)
                    that.ctx.lineTo(dot2.x, dot2.y)
                    that.ctx.lineTo(0, dot2.y)
                  }
                  that.ctx.closePath()
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                } else {
                  //右下至左上
                  if (dot3.x <= 0) {
                    res()
                    return
                  }
                  if (dot3.y > 0) {
                    dot3.y -= step
                    dot4.x -= step
                  } else {
                    dot3.x -= step
                    dot4.y -= step
                  }
                  that.ctx.save()
                  that.ctx.beginPath()
                  that.ctx.moveTo(400, 400)
                  if (dot3.x == 400) {
                    that.ctx.lineTo(dot3.x, dot3.y)
                    that.ctx.lineTo(dot4.x, dot4.y)
                  } else {
                    that.ctx.lineTo(400, 0)
                    that.ctx.lineTo(dot3.x, dot3.y)
                    that.ctx.lineTo(dot4.x, dot4.y)
                    that.ctx.lineTo(0, 400)
                  }
                  that.ctx.closePath()
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                }

                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              // that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 0.87
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //挤压过渡
    pushOut(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作：第二张进场
              let x = that.width / 2
              let y = that.height / 2
              ;(function two() {
                if (that.specialType == -1) {
                  if (y <= 0) {
                    // out(that.imgList[index1], that.imgList[index2])
                    res()
                    return
                  }
                  y -= 23
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w1,
                    h1,
                    0,
                    y,
                    that.width,
                    that.height - 2 * y
                  )
                } else {
                  if (x <= 0) {
                    res()
                    // out(that.imgList[index1], that.imgList[index2])
                    return
                  }
                  x -= 23
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    x,
                    0,
                    that.width - 2 * x,
                    that.height
                  )
                }
                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40) //回调内二方法
              })()
            } else {
              //第一部分动作：左右移动
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 0.88
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //直角过渡
    rightAngle(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              let xw1 = 100,
                xh1 = 0
              let xw2 = 300,
                xh2 = 400
              ;(function two() {
                if (that.specialType == -1) {
                  if (xh1 >= 400) {
                    res()
                    return
                  }
                  xw1 += 27
                  xh1 += 27
                  that.ctx.save()
                  that.ctx.beginPath()
                  that.ctx.moveTo(0, 0)
                  that.ctx.lineTo(xh1, 0)
                  that.ctx.lineTo(xw1, 200)
                  that.ctx.lineTo(xh1, 400)
                  that.ctx.lineTo(0, 400)
                  that.ctx.closePath()
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                } else {
                  //右下至左上
                  if (xh2 <= 0) {
                    res()
                    return
                  }
                  xw2 -= 27
                  xh2 -= 27
                  that.ctx.save()
                  that.ctx.beginPath()
                  that.ctx.moveTo(400, 0)
                  that.ctx.lineTo(xh2, 0)
                  that.ctx.lineTo(xw2, 200)
                  that.ctx.lineTo(xh2, 400)
                  that.ctx.lineTo(400, 400)
                  that.ctx.closePath()
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                }

                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              // that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 1
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    },
    //两侧飞入
    flyIn(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          that.specialType = -that.specialType
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 40) {
              //第二部分动作
              let rotate = 40
              let x0 = -300
              let y0 = -400
              ;(function two() {
                if (rotate <= 0) {
                  res()
                  return
                }
                rotate -= 5
                x0 += 37.5
                y0 += 50
                that.ctx.clearRect(0, 0, that.width, that.height)
                that.ctx.drawImage(
                  img1,
                  0,
                  0,
                  w1,
                  h1,
                  0,
                  0,
                  that.width,
                  that.height
                )
                that.ctx.save()
                if (that.specialType == -1) {
                  that.ctx.rotate((rotate * Math.PI) / 180)
                  that.ctx.translate(x0, y0)
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                } else {
                  that.ctx.rotate((-rotate * Math.PI) / 180)
                  that.ctx.translate(-x0, -y0)
                  that.ctx.drawImage(
                    img2,
                    0,
                    0,
                    w2,
                    h2,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                }

                that.twoTimeOut = setTimeout(() => {
                  two()
                }, 40)
              })()
            } else {
              //第一部分动作
              // that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.save()
              let step = that.getStep(move1)
              that.ctx.translate(step, 0)
              move1 += 0.86
              that.ctx.drawImage(
                img1,
                0,
                0,
                w1,
                h1,
                0,
                0,
                that.width,
                that.height
              )
              that.ctx.restore()
              that.oneTimeOut = setTimeout(() => {
                one()
              }, 40) //回调内方法
            }
          })()
        })(that.imgList[index1], that.imgList[index2])
      })
    }
  }
}
</script>
<style lang="scss">
// .player-wrap {
// }
</style>
