<template>
  <div class="player-wrap">
    <canvas id="player" :width="canvasWidth" height="400">
      Your browser does not support the HTML5 canvas tag.
    </canvas>
    <!-- <video
      style="border: 1px solid black;margin-left:100px;"
      :width="canvasWidth"
      height="400"
      controls="true"
      autoplay="true"
      id="video-mp4"
    ></video> -->
  </div>
</template>

<script>
// import FFmpeg from '@ffmpeg/ffmpeg'
import axios from 'axios'
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
    videoId: {
      type: Number,
      default: 1
    },
    videoNick: {
      type: String,
      default: ''
    },
    images: {
      type: Array,
      default() {
        return []
      }
    },
    isDaily: {
      type: Boolean,
      default: false
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
        videoBitsPerSecond: 2 * 1024 * 1024
      })

      const data = []
      recorder.ondataavailable = function(event) {
        if (event.data && event.data.size) {
          data.push(event.data)
        }
      }
      recorder.onstop = () => {
        // console.log('结束录制')
        //直接下载
        let aTemp = document.createElement('a')
        aTemp.href = URL.createObjectURL(
          new Blob(data, {
            type: 'video/mp4'
          })
        )
        let name = new Date().getTime()
        aTemp.download = '40TestVideo-JS' + name + '.mp4'
        aTemp.click()

        // dom 显示
        // let video = document.querySelector('#video-mp4')
        // video.src = URL.createObjectURL(
        //   new Blob(data, {
        //     type: 'video/mp4'
        //   })
        // )
        this.sendVideoToServer(data)
      }
      recorder.start()
      setTimeout(() => {
        recorder.stop()
      }, (this.videoDuration + 2) * 1000)
    },
    sendVideoToServer(data) {
      console.log('data', data)
      let formData = new FormData()
      let videoName = new Date().getTime() + '.mp4'
      let file = new window.File(data, videoName, {
        type: 'video/mp4'
      })
      formData.append('files', file)
      axios.defaults.headers = {
        'Content-Type': 'application/json;charset=UTF-8'
      }
      axios.defaults.timeout = 5000
      let baseUrl =
        '//ffmpeg-api.baobeituan.com:8080/bbt-video-cloud/BbtVideoCloudService/convertAndUploadVideo.post'
      console.log('this.isDaily', this.isDaily)
      if (this.isDaily == true) {
        baseUrl =
          '//120.78.152.235:7031/bbt-video-cloud/BbtVideoCloudService/convertAndUploadVideo.post'
      }
      let num = (parseFloat(this.images.length * 2.3).toFixed(1) + '').split(
        '.'
      )
      let str =
        '00:00:' +
        (num[0].length == 1 ? '0' + num[0] : num[0]) +
        '.' +
        num[1] * 100
      console.log('lenght:', this.images.length, '  str:', str)
      // let dataTemp = new Date()
      // let timeTemp = `${dataTemp.getHours()}:${dataTemp.getMinutes()}:${dataTemp.getSeconds()}`
      // let loggerStr = `请求时间：${timeTemp};`
      axios
        .post(
          baseUrl +
            '?musicUrl=' +
            // 'client/template/video/convertAndUpload.post?musicUrl=' +
            this.musicUrl +
            '&id=' +
            this.videoId +
            '&nick=' +
            this.videoNick +
            '&cutTime=' +
            str,
          formData
        )
        .then(res => {
          // loggerStr += `请求成功，res:${res}`
          try {
            if (res.data && res.status == 1) {
              console.log(res)
            }
          } catch (error) {
            console.error('BBT异常捕获', error)
          }
        })
        .catch(err => {
          // loggerStr += `请求失败，err:${err}`
          console.error('错误，', err)
        })
      // setTimeout(() => {
      // let a = document.createElement('a')
      // a.href = 'data:text/paint; utf-8,' + loggerStr
      // a.click()
      // }, 5500)
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

    getStep(move1, type) {
      if (type == 'new') {
        // 新方案 0 -> 20 / 0 -> -20
        return this.specialType * move1
      } else if (type == 'slow') {
        //计算左右位移 0 -> 5 -> 0 -> -5 -> 0
        let step
        if (move1 < 5) {
          step = move1
        } else if (move1 < 10) {
          step = 10 - move1
        } else if (move1 < 15) {
          step = 10 - move1
        } else if (move1 < 20) {
          step = -20 + move1
        }
        return step
      } else if (type == 'v2') {
        //计算 左/右 位移 0 -> 10 -> 0
        //            0-> -10 -> 0
        let step
        if (move1 < 10) {
          step = move1
        } else if (move1 < 20) {
          step = 20 - move1
        }
        return step
      } else {
        //计算左右位移 0 -> 10 -> 0 -> -10 -> 0
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
      }
    },

    // drawImage(img,sx,sy,swidth,sheight,x,y,width,height)
    //计算截取不同图片的区域坐标;  铺满、不放大、取中间区域
    getImgInfo(w, h) {
      let sx, sy, swidth, sheight
      if (this.mode == 1) {
        if (w == h) {
          return { sx: 0, sy: 0, swidth: w, sheight: h }
        } else if (w > h) {
          sheight = h
          swidth = h
          sx = (w - h) / 2
          sy = 0
        } else {
          sheight = w
          swidth = w
          sx = 0
          sy = (h - w) / 2
        }
      } else {
        if (w == h) {
          return { sx: w / 8, sy: 0, swidth: (w / 8) * 6, sheight: h }
        } else if (w / 3 > h / 4) {
          sheight = h
          swidth = (h / 4) * 3
          sx = (w - (h / 4) * 3) / 2
          sy = 0
        } else {
          sheight = (w / 3) * 4
          swidth = w
          sx = 0
          sy = (h - (w / 3) * 4) / 2
        }
      }
      return { sx, sy, swidth, sheight }
    },
    //上下放大
    upEnlarge(index1, index2) {
      let that = this
      // eslint-disable-next-line no-unused-vars
      let mode = that.mode
      // eslint-disable-next-line no-unused-vars
      return new Promise(function(res, rej) {
        ;(function out(img1, img2) {
          let h1 = img1.height
          let w1 = img1.width
          let h2 = img2.height
          let w2 = img2.width
          let width = that.width
          let height = that.height
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType
          let enlarge = 1.0
          that.ctx.clearRect(0, 0, width, height)
          ;(function one() {
            if (enlarge >= 1.1) {
              //第二部分动作
              let h0 = 0
              ;(function two() {
                if (h0 > height) {
                  res()
                  return
                }
                that.ctx.clearRect(0, 0, width, height)
                that.ctx.drawImage(
                  img1,
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
                  0,
                  0,
                  width,
                  height
                )
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
                  imgInfo2.sx,
                  imgInfo2.sy,
                  imgInfo2.swidth,
                  imgInfo2.sheight,
                  0,
                  y,
                  width,
                  height
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
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
                (-width * (enlarge - 1)) / 2,
                (-height * (enlarge - 1)) / 2,
                width,
                height
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          let alpha1 = 100,
            alpha2 = 0
          let move1 = 0
          that.specialType = -that.specialType
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
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
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
                  0,
                  0,
                  that.width,
                  that.height
                )
                that.ctx.globalAlpha = alpha2 / 100
                alpha2 += 7
                that.ctx.drawImage(
                  img2,
                  imgInfo2.sx,
                  imgInfo2.sy,
                  imgInfo2.swidth,
                  imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              that.ctx.translate(step * that.specialType, 0)
              move1 += 0.5
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
                0,
                0,
                that.width,
                that.height
              )
              let r = 0 //半径
              let x = that.mode == 2 ? 150 : 200,
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
                  that.ctx.arc(
                    that.mode == 2 ? 150 : 200,
                    200,
                    r,
                    0,
                    Math.PI * 2,
                    true
                  )
                  that.ctx.globalAlpha = 0
                  that.ctx.stroke()
                  that.ctx.globalAlpha = 1
                  that.ctx.clip()
                  that.ctx.drawImage(
                    img2,
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
                    0,
                    0,
                    that.width,
                    that.height
                  )
                  that.ctx.restore()
                } else {
                  if (y <= 0) {
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              that.ctx.translate(step * that.specialType, 0)
              move1 += 0.5
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
              //第二部分动作
              // let numX = 20
              // let numY = 20
              let num = 20
              let maxH = that.height / num
              let maxW = that.width / num
              let ratioW = imgInfo2.swidth / that.width
              let ratioH = imgInfo2.sheight / that.height
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
                  obj.sx = imgInfo2.sx + ratioW * maxW * i
                  obj.sy = imgInfo2.sy + ratioH * maxH * j
                  obj.swidth = ratioW * maxW
                  obj.sheight = ratioH * maxH

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
              let step = that.getStep(move1, 'v2')
              step *= that.specialType
              move1 += 0.45
              if (that.specialType == -1) {
                that.ctx.translate(step, 0)
                that.ctx.drawImage(
                  img1,
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
                  0,
                  0,
                  that.width,
                  that.height
                )
              } else {
                that.ctx.translate(0, step)
                that.ctx.drawImage(
                  img1,
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
              let num = 20
              let objH = 0
              let objW = 0
              let maxH = that.height / num
              let maxW = that.width / num
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
                  let ratioH = imgInfo2.sheight / that.height
                  for (let i = 0; i < num; i++) {
                    that.ctx.drawImage(
                      img2,
                      imgInfo2.sx,
                      imgInfo2.sy + ratioH * maxH * i,
                      imgInfo2.swidth,
                      ratioH * objH,
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
                  let ratioW = imgInfo2.swidth / that.width
                  for (let i = 0; i < num; i++) {
                    that.ctx.drawImage(
                      img2,
                      imgInfo2.sx + ratioW * maxW * i,
                      imgInfo2.sy,
                      ratioW * objW,
                      imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              // step *= that.specialType
              move1 += 0.52
              if (that.specialType == -1) {
                that.ctx.translate(step, 0)
                that.ctx.drawImage(
                  img1,
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
                  0,
                  0,
                  that.width,
                  that.height
                )
              } else {
                that.ctx.translate(0, step)
                that.ctx.drawImage(
                  img1,
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
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
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
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
                  imgInfo2.sx,
                  imgInfo2.sy,
                  imgInfo2.swidth,
                  imgInfo2.sheight,
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
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType
          let step = 50
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              step *= that.specialType
              that.ctx.translate(step, 0)
              move1 += 0.43
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType //-1 上下; 1 左右;
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
                  x -= that.mode == 2 ? 20 : 23
                  that.ctx.drawImage(
                    img2,
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              step *= that.specialType
              that.ctx.translate(step, 0)
              move1 += 0.44
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
              //第二部分动作
              that.ctx.clearRect(0, 0, that.width, that.height)
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              step *= that.specialType
              that.ctx.translate(step, 0)
              move1 += 0.5
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
          let imgInfo1 = that.getImgInfo(w1, h1)
          let imgInfo2 = that.getImgInfo(w2, h2)
          that.specialType = -that.specialType
          let move1 = 0
          that.ctx.clearRect(0, 0, that.width, that.height)
          ;(function one() {
            if (move1 >= 20) {
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
                  imgInfo1.sx,
                  imgInfo1.sy,
                  imgInfo1.swidth,
                  imgInfo1.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
                    imgInfo2.sx,
                    imgInfo2.sy,
                    imgInfo2.swidth,
                    imgInfo2.sheight,
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
              let step = that.getStep(move1, 'v2')
              step *= that.specialType
              that.ctx.translate(step, 0)
              move1 += 0.43
              that.ctx.drawImage(
                img1,
                imgInfo1.sx,
                imgInfo1.sy,
                imgInfo1.swidth,
                imgInfo1.sheight,
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
