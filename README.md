# settlements

> A procedural village generator that builds houses, large wheat fields, and windmills!

This project is a Forge mod submission for the [Generative Design in Minecraft](http://gendesignmc.engineering.nyu.edu/) Settlement Generation Competition.

![Screenshot](https://github.com/vberlier/settlements/raw/master/screenshot.png)

> Check out the [Imgur album](https://imgur.com/a/ua4z2gt) for more screenshots.

## Download

- [Download for Minecraft 1.12](https://github.com/vberlier/settlements/raw/master/submissions/GDMC%202019%20-%20Valentin%20Berlier%202/settlements-0.1.1.jar)

## Usage

After installing the mod, you'll be able to invoke the generator by running the `BuildSettlement` command. The generator is not integrated into the world generation.

```
/BuildSettlement <x1> <y1> <z1> <x2> <y2> <z2>
```

The command typically takes 10 to 20 seconds to run but it depends on the size of the selection. I recommend opening the output log when running Minecraft to make sure that the generator doesn't get stuck.

**:warning: Disclaimer :warning:**

This is a proof-of-concept. The generator is far from optimized and lacks a lot of features. The code is also... really bad. Try not to look in there for too long :grimacing:

---

License - [MIT](https://github.com/vberlier/settlements/blob/master/LICENSE)
