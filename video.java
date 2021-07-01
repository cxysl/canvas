public static void main(String[] args) {
		System.out.println("测试");
		Date startTime = new Date();
		String filePath="C:\\Users\\admin\\Downloads\\40Test.webm";
		File file=new File(filePath);
		FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
		String fileName = null;
		Frame captured_frame = null;
		FFmpegFrameRecorder recorder = null;
		try {
			frameGrabber.start();
			fileName = file.getAbsolutePath() + "__40_low.mp4";
			recorder = new FFmpegFrameRecorder(fileName, frameGrabber.getImageWidth(), frameGrabber.getImageHeight(), frameGrabber.getAudioChannels());
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.setFormat("mp4");
			System.out.println(frameGrabber.toString());
			System.out.println("getSampleRate," + frameGrabber.getSampleRate());
			System.out.println("getFrameRate," + frameGrabber.getFrameRate());
			recorder.setSampleRate(frameGrabber.getSampleRate());
			recorder.setFrameRate(frameGrabber.getFrameRate());
			recorder.setVideoBitrate(1 * 1024 * 1024);
			recorder.start();
			boolean bool=false;
			while ((captured_frame = frameGrabber.grabFrame()) != null) {
				try {
//					System.out.println("时长2==="+captured_frame.timestamp);
					recorder.setTimestamp(frameGrabber.getTimestamp());
					recorder.record(captured_frame);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
			recorder.stop();
			recorder.release();
			frameGrabber.stop();
			Date endTime = new Date();
			System.out.println("时长：" + (endTime.getTime()-startTime.getTime())/1000.0);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static void main12138(String[] args) {

		String home = "C:\\Users\\admin\\Downloads\\";
		String imageInput = home + "40Test_noMusic.mp4";
		Date startTime = new Date();
		File file1=new File(imageInput);
		FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(file1);
		System.out.println("原视频对象" + videoGrabber);

		try {
			videoGrabber.start();
			String audioInput = home + "22.mp3";
			File file2 =new File(audioInput);
			FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(file2);
			audioGrabber.start();
			String outputPath = home + "endVideo40-04.mp4";
			// 流媒体输出地址，分辨率（长，高），是否录制音频（0:单声道/1:立体声）
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, videoGrabber.getImageWidth(), videoGrabber.getImageHeight(), 1);
			recorder.setInterleaved(true);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.setFormat("mp4");
			recorder.setVideoBitrate(1 * 1024 * 1024);
			recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
			int frameRate = 1000;
			recorder.setFrameRate(frameRate);
			recorder.setGopSize(frameRate * 2);
			recorder.setAudioOption("crf", "0");
			recorder.setAudioQuality(0);// 最高质量
			recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			recorder.setAudioChannels(2);
			recorder.setSampleRate(44100);
			recorder.start();
			long videoTime = videoGrabber.getLengthInTime();
			Frame imageFrame = null;
			while ((imageFrame = videoGrabber.grabImage()) != null) {
				recorder.record(imageFrame);
			}
			Frame sampleFrame = null;
			while ((sampleFrame = audioGrabber.grabSamples()) != null) {
				recorder.record(sampleFrame);
				if (audioGrabber.getTimestamp() >= videoTime) {
					break;
				}
			}
			recorder.close();
			audioGrabber.close();
			videoGrabber.close();
		} catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
			e.printStackTrace();
		}

		Date endTime = new Date();
		System.out.println("合成时长：" + (endTime.getTime()-startTime.getTime())/1000.0);
	}