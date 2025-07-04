<?xml version="1.0" encoding="UTF-8"?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" name="New Energy System" description="" id="a31e067d-f21c-4cf5-bada-b8c311c46836" version="2" esdlVersion="v2303">
  <instance id="46200519-727b-4a9c-9b22-1d42830d5ca6" name="Untitled instance">
    <area id="fc3c534e-2ff7-46b7-b6c1-59b3ec3e39d0" name="Untitled area">
      <asset xsi:type="esdl:Joint" id="5104b21c-5e62-42e8-8b24-95cf27bdeed8" name="Joint_5104">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="53.91081008725412" lon="4.691162109375001"/>
        <port xsi:type="esdl:InPort" id="b871d704-eef2-48e6-aeb3-7c36cf582851" name="In"/>
        <port xsi:type="esdl:OutPort" id="0588487c-e75b-4fab-a4df-0c9acd47c57f" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Joint" id="7ebfc1bf-4b18-4f32-aca2-73262d34bc34" name="Joint_7ebf">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="54.07228265560388" lon="4.075927734375001"/>
        <port xsi:type="esdl:InPort" id="7bc7d21e-b722-4475-8677-24a4d9cf8ecf" name="In" connectedTo="dd29032b-f694-4026-8383-f53745f4ad3e"/>
        <port xsi:type="esdl:OutPort" id="8e69e86a-6165-4c79-8e3d-8d7448bebcb1" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Joint" id="4cb8d11d-6a73-4918-8f52-fbe4777294af" name="Joint_4cb8">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="54.7246201949245" lon="5.053710937500001"/>
        <port xsi:type="esdl:InPort" id="5269511c-a174-4d55-b48c-3514bb78b5b2" name="In"/>
        <port xsi:type="esdl:OutPort" id="a4dbad8b-4970-4ea5-bb60-d940e84bf39a" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Joint" id="36802784-08d8-45cc-a9ca-b9c0d537b719" name="Joint_3680">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="55.25407706707272" lon="4.801025390625001"/>
        <port xsi:type="esdl:InPort" id="e1d60e2c-9d4d-4e54-9511-4922d1e8a667" name="In"/>
        <port xsi:type="esdl:OutPort" id="e912738c-e8b9-4f14-8ba1-52e618d7fcce" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Joint" id="c6cbdad7-2609-4b82-8698-c227017c10fe" name="Joint_c6cb">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="55.26033810292462" lon="3.40576171875"/>
        <port xsi:type="esdl:InPort" id="63c79b9b-6ddc-4102-b2c9-60be2ec2e94f" name="In"/>
        <port xsi:type="esdl:OutPort" id="fef84f08-1add-49ae-96f4-d8c3ada12b02" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Pipe" id="d7ef3938-5ffd-4638-8cf5-9dd4b82ee0bb" name="Pipe_d7ef" length="162057.8">
        <geometry xsi:type="esdl:Line" CRS="WGS84">
          <point lat="52.8492298820527" lon="4.702148437500001"/>
          <point lat="52.9751081817353" lon="4.361572265625001"/>
          <point lat="53.23234504341259" lon="3.9770507812500004"/>
          <point lat="53.657661020298" lon="3.8342285156250004"/>
          <point lat="54.07107394110007" lon="4.075241088867188"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="df515cc0-7e74-4ff6-830d-679662ff76e5" name="In"/>
        <port xsi:type="esdl:OutPort" id="baed40f0-24c3-4bf8-b6b4-49550cb2c9b2" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Pipe" id="f648035d-d20a-406f-addc-1ab278cac446" name="Pipe_f648" length="43302.6">
        <geometry xsi:type="esdl:Line">
          <point lat="53.91242791331529" lon="4.682922363281251"/>
          <point lat="54.07177902883705" lon="4.078502655029298"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="b2212f79-a017-4201-830e-e73781182887" name="In"/>
        <port xsi:type="esdl:OutPort" id="41a57032-86a2-437c-915c-f7bcbb1350cd" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Pipe" id="eea70f9d-cf1b-4556-9d22-3cca2e00d658" name="Pipe_eea7" length="168951.1">
        <geometry xsi:type="esdl:Line" CRS="WGS84">
          <point lat="53.46843072001161" lon="6.789550781250001"/>
          <point lat="53.51418452077113" lon="6.5423583984375"/>
          <point lat="53.94638778530838" lon="6.2347412109375"/>
          <point lat="53.914045676705264" lon="4.713134765625"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="cf7e241b-4484-4fac-893b-9ea7c359617c" name="In"/>
        <port xsi:type="esdl:OutPort" id="eb137585-2c3b-4dac-af39-ae68e0bf4a60" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Pipe" id="9996c37c-dc08-4736-a82b-05da3e3eb94e" name="Pipe_9996" length="96075.1">
        <geometry xsi:type="esdl:Line">
          <point lat="54.07379349924954" lon="4.077301025390626"/>
          <point lat="54.34214886448344" lon="4.339599609375001"/>
          <point lat="54.718275018302315" lon="5.042724609375001"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="66d00e42-6ac5-4b34-9818-d3eb2a9523e6" name="In"/>
        <port xsi:type="esdl:OutPort" id="e0701a41-4c93-4d06-9524-042964d1bef8" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Pipe" id="763004d9-86ee-449c-abf1-851061130d15" name="Pipe_7630" length="121918.4">
        <geometry xsi:type="esdl:Line" CRS="WGS84">
          <point lat="54.73255026902896" lon="5.05645751953125"/>
          <point lat="54.77534585936449" lon="5.031738281250001"/>
          <point lat="55.26659815231191" lon="3.636474609375"/>
          <point lat="55.26659815231191" lon="3.4387207031250004"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="5dda89da-ae25-4136-b653-e44ea6365b61" name="In"/>
        <port xsi:type="esdl:OutPort" id="88fa7741-8e5e-40b5-86ce-d6c54894eecd" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Electrolyzer" id="b197ef61-0662-4522-aea1-0bb95552a6da" name="Electrolyzer_b197">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="54.07349133491893" lon="4.080219268798829"/>
        <port xsi:type="esdl:InPort" id="06cae49a-378c-4b94-9e15-e87f46de0263" name="In" connectedTo="8b44df6d-c49d-4fdf-9584-2103a45093c0 302c5e30-2aae-4bdb-8e40-89388a39fbfd"/>
        <port xsi:type="esdl:OutPort" id="dd29032b-f694-4026-8383-f53745f4ad3e" name="Out" connectedTo="7bc7d21e-b722-4475-8677-24a4d9cf8ecf"/>
      </asset>
      <asset xsi:type="esdl:CCS" id="2a32a6bd-1c08-4eab-b939-3fa0986f87ed" name="CCS_2a32" surfaceArea="143805">
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior>
            <point lat="54.07802356895898" lon="4.070434570312501"/>
            <point lat="54.0748006978013" lon="4.065284729003907"/>
            <point lat="54.07288699965997" lon="4.069061279296876"/>
            <point lat="54.07681502159739" lon="4.073524475097657"/>
          </exterior>
        </geometry>
        <port xsi:type="esdl:InPort" id="fa6d89a1-4962-44b0-beae-f62deb9d8b8f" name="In"/>
        <port xsi:type="esdl:OutPort" id="d14fd33b-83b5-4a61-85e0-b6de3f14283f" name="Out"/>
      </asset>
      <asset xsi:type="esdl:CCS" id="3359c717-eaac-4cad-862c-805256293e34" name="CCS_3359" surfaceArea="112044">
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior>
            <point lat="54.07782214684222" lon="4.085154533386231"/>
            <point lat="54.07540500520312" lon="4.08219337463379"/>
            <point lat="54.07351651536383" lon="4.087815284729005"/>
            <point lat="54.07580787191716" lon="4.089102745056153"/>
          </exterior>
        </geometry>
        <port xsi:type="esdl:InPort" id="142782d2-f9bf-4ed6-baa6-2e4d1632940e" name="In"/>
        <port xsi:type="esdl:OutPort" id="ca7a080b-16b7-4aeb-80d4-28e049c98845" name="Out"/>
      </asset>
      <asset xsi:type="esdl:GasStorage" id="57cc1f37-6c07-4bb2-b411-b3abde7717d0" name="GasStorage_57cc">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="54.070117017161635" lon="4.077129364013673"/>
        <port xsi:type="esdl:InPort" id="07200830-d65c-4524-b385-564dddaeb2c6" name="In"/>
        <port xsi:type="esdl:OutPort" id="b2695ee9-ea47-4b93-9b4c-5b6d833070f3" name="Out"/>
      </asset>
      <asset xsi:type="esdl:WindPark" id="79f06820-4b43-4bf1-bfc3-5abf1a4d8d9b" name="WindPark_79f0" surfaceArea="850713634">
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior>
            <point lat="54.41573362292812" lon="3.9990234375000004"/>
            <point lat="54.01099683495081" lon="3.8342285156250004"/>
            <point lat="54.01099683495081" lon="4.136352539062501"/>
            <point lat="54.409340183768315" lon="4.279174804687501"/>
          </exterior>
        </geometry>
        <port xsi:type="esdl:OutPort" id="8b44df6d-c49d-4fdf-9584-2103a45093c0" name="Out" connectedTo="06cae49a-378c-4b94-9e15-e87f46de0263"/>
        <port xsi:type="esdl:InPort" id="fb5f44c7-d8fc-4ba7-9d6e-8d562070695c" name="In"/>
      </asset>
      <asset xsi:type="esdl:WindPark" id="47459ebf-8df5-440c-8a11-59fe2951bf84" name="WindPark_4745" surfaceArea="1008036326">
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior>
            <point lat="54.01422465756089" lon="4.185791015625001"/>
            <point lat="54.64523407607479" lon="4.592285156250001"/>
            <point lat="54.708755390956064" lon="4.888916015625001"/>
            <point lat="54.03681240523652" lon="4.378051757812501"/>
          </exterior>
        </geometry>
        <port xsi:type="esdl:OutPort" id="302c5e30-2aae-4bdb-8e40-89388a39fbfd" name="Out" connectedTo="06cae49a-378c-4b94-9e15-e87f46de0263"/>
        <port xsi:type="esdl:InPort" id="471d2ac6-f2f4-431b-b274-f29bc5011c97" name="In"/>
      </asset>
    </area>
  </instance>
</esdl:EnergySystem>
